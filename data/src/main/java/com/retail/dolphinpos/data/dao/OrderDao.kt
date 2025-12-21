package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retail.dolphinpos.data.entities.order.OrderEntity

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("SELECT * FROM orders ORDER BY created_at DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE is_synced = :isSynced ORDER BY created_at DESC")
    suspend fun getOrdersBySyncStatus(isSynced: Boolean): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE order_source = :source ORDER BY created_at DESC")
    suspend fun getOrdersBySource(source: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE order_source = 'local' AND is_synced = 0")
    suspend fun getUnsyncedLocalOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE order_source = 'api' AND is_synced = :isSynced ORDER BY created_at DESC")
    suspend fun getApiOrdersBySyncStatus(isSynced: Boolean): List<OrderEntity>

    @Query("SELECT * FROM orders ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestOrder(): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE server_id = :serverId")
    suspend fun getOrderByServerId(serverId: Int): OrderEntity?

    @Query("SELECT * FROM orders WHERE order_no = :orderNumber")
    suspend fun getOrderByOrderNumber(orderNumber: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE store_id = :storeId ORDER BY created_at DESC")
    suspend fun getOrdersByStoreId(storeId: Int): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE location_id = :locationId ORDER BY created_at DESC")
    suspend fun getOrdersByLocationId(locationId: Int): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getOrdersByUserId(userId: Int): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE store_id = :storeId AND (order_no LIKE '%' || :keyword || '%' OR payment_method LIKE '%' || :keyword || '%') ORDER BY created_at DESC")
    suspend fun searchOrdersByStoreId(storeId: Int, keyword: String): List<OrderEntity>
    
    /**
     * Gets all orders for a specific batch ID.
     * Used to find orders that belong to a batch for syncing.
     */
    @Query("SELECT * FROM orders WHERE batch_id = :batchId ORDER BY created_at ASC")
    suspend fun getOrdersByBatchId(batchId: String): List<OrderEntity>
    
    /**
     * Gets all unsynced orders for batches eligible for order sync.
     * Orders can only be synced if batch start is synced (START_SYNCED or later, but not FAILED).
     * 
     * NOTE: This query is deprecated in favor of filtering in code using getBatchesEligibleForOrderSync()
     * to ensure correct sync status checking.
     */
    @Deprecated("Use getUnsyncedLocalOrders() and filter by eligible batches in code")
    @Query("""
        SELECT o.* FROM orders o
        INNER JOIN batch b ON o.batch_id = b.batchId
        WHERE o.is_synced = 0 
        AND b.syncStatus IN ('START_SYNCED', 'CLOSE_PENDING', 'CLOSE_SYNCED')
        ORDER BY o.created_at ASC
    """)
    suspend fun getUnsyncedOrdersForSyncedBatches(): List<OrderEntity>
    
    /**
     * Gets unsynced orders for a specific batch ID.
     * Used when syncing orders for a specific batch.
     */
    @Query("""
        SELECT o.* FROM orders o
        WHERE o.batch_id = :batchId 
        AND o.is_synced = 0
        ORDER BY o.created_at ASC
    """)
    suspend fun getUnsyncedOrdersByBatchId(batchId: String): List<OrderEntity>

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Long)

    // Upsert based on serverId (for API orders)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: OrderEntity): Long

    // Clean up duplicate orders - keep the order with the lowest id for each order_no
    @Query("""
        DELETE FROM orders 
        WHERE id NOT IN (
            SELECT MIN(id) 
            FROM orders 
            GROUP BY order_no
        )
    """)
    suspend fun removeDuplicateOrders()
}

