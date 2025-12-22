package com.retail.dolphinpos.data.repositories.sync

import com.retail.dolphinpos.data.dao.OrderDao
import com.retail.dolphinpos.data.dao.SyncCommandDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.entities.order.OrderEntity
import com.retail.dolphinpos.data.entities.order.OrderSyncStatus
import com.retail.dolphinpos.data.entities.sync.CommandType
import com.retail.dolphinpos.data.entities.sync.CommandStatus
import com.retail.dolphinpos.data.entities.sync.SyncCommandEntity
import com.retail.dolphinpos.data.entities.user.BatchEntity
import com.retail.dolphinpos.data.entities.user.BatchSyncStatus
import com.retail.dolphinpos.data.room.DolphinDatabase

/**
 * Repository for POS sync operations
 * Handles offline-first batch and order operations with command queue
 */
class PosSyncRepository(
    private val database: DolphinDatabase
) {
    
    private val userDao: UserDao = database.userDao()
    private val orderDao: OrderDao = database.orderDao()
    private val syncCommandDao: SyncCommandDao = database.syncCommandDao()
    
    /**
     * Start a new batch (offline)
     * - Enforces only one active batch
     * - Creates BatchEntity with status ACTIVE_LOCAL
     * - Enqueues OPEN_BATCH command
     * 
     * @param batchId Client-generated batch ID (e.g., "BATCH_S1L1R1U1-1234567890")
     * @param userId Cashier user ID
     * @param storeId Store ID
     * @param registerId Register ID
     * @param locationId Location ID
     * @param startingCashAmount Starting cash amount
     * @throws IllegalStateException if a batch is already active
     */
    suspend fun startBatch(
        batchId: String,
        userId: Int?,
        storeId: Int?,
        registerId: Int?,
        locationId: Int?,
        startingCashAmount: Double
    ) {
        // Check if there's already an active batch
        val activeBatch = userDao.getAllBatches().firstOrNull { 
            it.syncStatus == BatchSyncStatus.ACTIVE_LOCAL 
        }
        
        if (activeBatch != null) {
            throw IllegalStateException("A batch is already active. Please close the current batch before opening a new one.")
        }
        
        // Create batch entity
        val batchEntity = BatchEntity(
            batchNo = batchId,
            userId = userId,
            storeId = storeId,
            registerId = registerId,
            locationId = locationId,
            startingCashAmount = startingCashAmount,
            startedAt = System.currentTimeMillis(),
            closedAt = null,
            closingCashAmount = null,
            syncStatus = BatchSyncStatus.ACTIVE_LOCAL,
            isSynced = false
        )
        
        // Insert batch
        userDao.insertBatchDetails(batchEntity)
        
        // Enqueue OPEN_BATCH command (sequence generation is atomic)
        val sequence = syncCommandDao.getNextSequence()
        val command = SyncCommandEntity(
            sequence = sequence,
            type = CommandType.OPEN_BATCH,
            batchId = batchId,
            orderId = null,
            status = CommandStatus.PENDING,
            attempts = 0,
            lastError = null,
            createdAt = System.currentTimeMillis(),
            idempotencyKey = batchId
        )
        syncCommandDao.insertCommand(command)
    }
    
    /**
     * Place an order (offline)
     * - Creates OrderEntity with status LOCAL_ONLY
     * - Enqueues CREATE_ORDER command
     * 
     * @param orderId Client-generated order ID (orderNumber)
     * @param batchId Batch ID this order belongs to
     * @param orderEntity Order entity to save
     */
    suspend fun placeOrder(
        orderId: String,
        batchId: String,
        orderEntity: OrderEntity
    ) {
        // Update order entity with sync status and ensure it has the correct orderId/batchId
        val orderWithStatus = orderEntity.copy(
            orderNumber = orderId,
            batchNo = batchId,
            syncStatus = OrderSyncStatus.LOCAL_ONLY,
            isSynced = false
        )
        
        // Insert order
        orderDao.insertOrder(orderWithStatus)
        
        // Enqueue CREATE_ORDER command (sequence generation is atomic)
        val sequence = syncCommandDao.getNextSequence()
        val command = SyncCommandEntity(
            sequence = sequence,
            type = CommandType.CREATE_ORDER,
            batchId = batchId,
            orderId = orderId,
            status = CommandStatus.PENDING,
            attempts = 0,
            lastError = null,
            createdAt = System.currentTimeMillis(),
            idempotencyKey = orderId
        )
        syncCommandDao.insertCommand(command)
    }
    
    /**
     * Close a batch (offline)
     * - Updates BatchEntity status to CLOSED_LOCAL
     * - Enqueues CLOSE_BATCH command
     * 
     * @param batchId Batch ID to close
     * @param closingCashAmount Closing cash amount
     * @throws IllegalStateException if batch not found or already closed
     */
    suspend fun closeBatch(
        batchId: String,
        closingCashAmount: Double?
    ) {
        // Get batch by batchNo (batchId)
        val batch = userDao.getBatchByBatchNo(batchId)
            ?: throw IllegalStateException("Batch not found: $batchId")
        
        if (batch.closedAt != null || batch.syncStatus == BatchSyncStatus.CLOSED_LOCAL || batch.syncStatus == BatchSyncStatus.SYNCED_CLOSED) {
            throw IllegalStateException("Batch is already closed: $batchId")
        }
        
        // Update batch
        val updatedBatch = batch.copy(
            closedAt = System.currentTimeMillis(),
            closingCashAmount = closingCashAmount,
            syncStatus = BatchSyncStatus.CLOSED_LOCAL
        )
        userDao.updateBatch(updatedBatch)
        
        // Enqueue CLOSE_BATCH command (sequence generation is atomic)
        val sequence = syncCommandDao.getNextSequence()
        val command = SyncCommandEntity(
            sequence = sequence,
            type = CommandType.CLOSE_BATCH,
            batchId = batchId,
            orderId = null,
            status = CommandStatus.PENDING,
            attempts = 0,
            lastError = null,
            createdAt = System.currentTimeMillis(),
            idempotencyKey = batchId
        )
        syncCommandDao.insertCommand(command)
    }
    
    /**
     * Get active batch (if any)
     */
    suspend fun getActiveBatch(): BatchEntity? {
        return userDao.getAllBatches().firstOrNull { 
            it.syncStatus == BatchSyncStatus.ACTIVE_LOCAL 
        }
    }
    
    /**
     * Get batch by batch ID
     */
    suspend fun getBatchByBatchId(batchId: String): BatchEntity? {
        return userDao.getBatchByBatchNo(batchId)
    }
}

