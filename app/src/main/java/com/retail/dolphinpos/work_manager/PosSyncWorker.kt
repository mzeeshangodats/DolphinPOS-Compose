package com.retail.dolphinpos.work_manager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.OrderDao
import com.retail.dolphinpos.data.dao.SyncCommandDao
import com.retail.dolphinpos.data.dao.SyncLockDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.entities.order.OrderEntity
import com.retail.dolphinpos.data.entities.order.OrderSyncStatus
import com.retail.dolphinpos.data.entities.sync.CommandStatus
import com.retail.dolphinpos.data.entities.sync.CommandType
import com.retail.dolphinpos.data.entities.sync.SyncCommandEntity
import com.retail.dolphinpos.data.entities.user.BatchSyncStatus
import com.retail.dolphinpos.data.repositories.order.OrderRepositoryImpl
import com.retail.dolphinpos.data.repositories.refund.RefundRepositoryImpl
import com.retail.dolphinpos.data.dao.RefundDao
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.mapper.RefundMapper
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

@HiltWorker
class PosSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncCommandDao: SyncCommandDao,
    private val syncLockDao: SyncLockDao,
    private val userDao: UserDao,
    private val orderDao: OrderDao,
    private val apiService: ApiService,
    private val orderRepository: OrderRepositoryImpl,
    private val refundRepository: RefundRepositoryImpl,
    private val refundDao: RefundDao,
    private val gson: Gson
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PosSyncWorker"
        private const val LOCK_TIMEOUT_MS = 300000L // 5 minutes
    }

    override suspend fun doWork(): Result {
        val workerId = UUID.randomUUID().toString()
        Log.d(TAG, "Starting sync worker: $workerId")

        return try {
            // Step 1: Acquire lock
            val lockAcquired = syncLockDao.tryAcquireLock(workerId, LOCK_TIMEOUT_MS)
            if (!lockAcquired) {
                Log.d(TAG, "Could not acquire lock, another worker may be running")
                return Result.retry()
            }

            try {
                // Step 2: Recovery - reset any RUNNING commands to PENDING (from crash)
                syncCommandDao.resetRunningToPending()
                Log.d(TAG, "Reset any RUNNING commands to PENDING")

                // Step 3: Process commands sequentially
                var processedCount = 0
                while (true) {
                    val command = syncCommandDao.getNextPendingCommand()
                    if (command == null) {
                        // No more pending commands
                        Log.d(TAG, "No more pending commands. Processed $processedCount commands.")
                        break
                    }

                    Log.d(TAG, "Processing command: ${command.type}, sequence: ${command.sequence}, id: ${command.id}")

                    // Mark command as RUNNING
                    val runningCommand = command.copy(status = CommandStatus.RUNNING)
                    syncCommandDao.updateCommand(runningCommand)

                    try {
                        // Execute command based on type
                        when (command.type) {
                            CommandType.OPEN_BATCH -> executeOpenBatch(command)
                            CommandType.CREATE_ORDER -> executeCreateOrder(command)
                            CommandType.CLOSE_BATCH -> executeCloseBatch(command)
                            CommandType.CREATE_REFUND -> executeCreateRefund(command)
                        }

                        // Mark command as DONE
                        val doneCommand = runningCommand.copy(
                            status = CommandStatus.DONE,
                            attempts = command.attempts + 1
                        )
                        syncCommandDao.updateCommand(doneCommand)
                        processedCount++
                        Log.d(TAG, "Command ${command.id} completed successfully")

                    } catch (e: Exception) {
                        Log.e(TAG, "Command ${command.id} failed: ${e.message}", e)
                        
                        // Mark command as FAILED and stop processing
                        val failedCommand = runningCommand.copy(
                            status = CommandStatus.FAILED,
                            attempts = command.attempts + 1,
                            lastError = e.message ?: "Unknown error"
                        )
                        syncCommandDao.updateCommand(failedCommand)

                        // Stop processing on error (don't process newer commands)
                        Log.e(TAG, "Stopping sync due to command failure")
                        return Result.retry() // WorkManager will retry with exponential backoff
                    }
                }

                Result.success()

            } finally {
                // Always release lock
                syncLockDao.releaseLock()
                Log.d(TAG, "Released lock")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in doWork: ${e.message}", e)
            syncLockDao.releaseLock()
            Result.retry()
        }
    }

    /**
     * Execute OPEN_BATCH command
     */
    private suspend fun executeOpenBatch(command: SyncCommandEntity) {
        val batchId = command.batchId ?: throw IllegalArgumentException("batchId is null for OPEN_BATCH command")

        // Get batch from database
        val batch = userDao.getBatchByBatchNo(batchId)
            ?: throw IllegalStateException("Batch not found: $batchId")

        // Create API request
        val request = BatchOpenRequest(
            batchNo = batchId,
            storeId = batch.storeId ?: throw IllegalArgumentException("storeId is null"),
            userId = batch.userId ?: throw IllegalArgumentException("userId is null"),
            locationId = batch.locationId ?: throw IllegalArgumentException("locationId is null"),
            storeRegisterId = batch.registerId,
            startingCashAmount = batch.startingCashAmount
        )

        // Call API (idempotent - backend should handle if already exists)
        val response = apiService.batchOpen(request)

        // Update batch status to SYNCED_OPEN
        val updatedBatch = batch.copy(syncStatus = BatchSyncStatus.SYNCED_OPEN)
        userDao.updateBatch(updatedBatch)

        Log.d(TAG, "Batch opened successfully: $batchId")
    }

    /**
     * Execute CREATE_ORDER command
     */
    private suspend fun executeCreateOrder(command: SyncCommandEntity) {
        val orderId = command.orderId ?: throw IllegalArgumentException("orderId is null for CREATE_ORDER command")
        val batchId = command.batchId ?: throw IllegalArgumentException("batchId is null for CREATE_ORDER command")

        // Verify batch is synced open (FIFO should ensure this, but check anyway)
        val batch = userDao.getBatchByBatchNo(batchId)
        if (batch == null || batch.syncStatus != BatchSyncStatus.SYNCED_OPEN) {
            throw IllegalStateException("Batch $batchId is not in SYNCED_OPEN state. Current status: ${batch?.syncStatus}")
        }

        // Get order from database
        val order = orderDao.getOrderByOrderNumber(orderId)
            ?: throw IllegalStateException("Order not found: $orderId")

        // Sync order to server using existing repository method
        val result = orderRepository.syncOrderToServer(order)
        result.getOrThrow() // Throw exception if failed

        // Update order syncStatus to SYNCED (repository updates isSynced but not syncStatus)
        val updatedOrder = order.copy(syncStatus = OrderSyncStatus.SYNCED)
        orderDao.updateOrder(updatedOrder)

        Log.d(TAG, "Order created successfully: $orderId")
    }

    /**
     * Execute CLOSE_BATCH command
     */
    private suspend fun executeCloseBatch(command: SyncCommandEntity) {
        val batchId = command.batchId ?: throw IllegalArgumentException("batchId is null for CLOSE_BATCH command")

        // Get batch from database
        val batch = userDao.getBatchByBatchNo(batchId)
            ?: throw IllegalStateException("Batch not found: $batchId")

        // Verify all orders for this batch are synced (optional check, FIFO should ensure this)
        val orders = orderDao.getOrdersByStoreId(batch.storeId ?: 0)
        val unsyncedOrders = orders.filter { 
            it.batchNo == batchId && it.syncStatus != OrderSyncStatus.SYNCED 
        }
        if (unsyncedOrders.isNotEmpty()) {
            Log.w(TAG, "Warning: Batch $batchId has ${unsyncedOrders.size} unsynced orders. Closing anyway due to FIFO guarantee.")
        }

        // Create API request
        // Note: BatchCloseRequest may need additional fields - adjust based on actual API requirements
        val request = BatchCloseRequest(
            cashierId = batch.userId ?: throw IllegalArgumentException("userId is null"),
            closedBy = batch.userId ?: throw IllegalArgumentException("userId is null"),
            closingCashAmount = batch.closingCashAmount ?: batch.startingCashAmount,
            locationId = batch.locationId ?: throw IllegalArgumentException("locationId is null"),
            orders = emptyList(), // Add abandon carts if needed
            paxBatchNo = "", // Add if needed
            storeId = batch.storeId ?: throw IllegalArgumentException("storeId is null")
        )

        // Call API (idempotent - backend should handle if already closed)
        val response = apiService.batchClose(batchId, request)

        // Update batch status to SYNCED_CLOSED
        val updatedBatch = batch.copy(syncStatus = BatchSyncStatus.SYNCED_CLOSED)
        userDao.updateBatch(updatedBatch)

        Log.d(TAG, "Batch closed successfully: $batchId")
    }
    
    /**
     * Execute CREATE_REFUND command
     */
    private suspend fun executeCreateRefund(command: SyncCommandEntity) {
        val refundId = command.orderId ?: throw IllegalArgumentException("refundId is null for CREATE_REFUND command")
        
        // Get refund from database
        val refund = refundDao.getRefundByRefundId(refundId)
            ?: throw IllegalStateException("Refund not found: $refundId")
        
        // Get order to get server ID
        val order = orderDao.getOrderByOrderNumber(refund.orderNo)
            ?: throw IllegalStateException("Order not found: ${refund.orderNo}")
        
        val serverOrderId = order.serverId
            ?: throw IllegalStateException("Order not synced to server yet: ${refund.orderNo}")
        
        Log.d(TAG, "Syncing refund $refundId to server...")
        
        // Sync refund to server using repository
        val result = refundRepository.syncRefundToServer(
            RefundMapper.toRefund(refund)
        )
        result.getOrThrow() // Throw exception if failed
        
        Log.d(TAG, "Refund $refundId synced successfully")
    }
}

