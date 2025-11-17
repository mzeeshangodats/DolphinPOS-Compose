package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retail.dolphinpos.data.entities.order.PendingOrderEntity

@Dao
interface PendingOrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingOrder(order: PendingOrderEntity): Long

    @Query("SELECT * FROM pending_orders WHERE is_synced = 0")
    suspend fun getUnsyncedOrders(): List<PendingOrderEntity>

    @Query("SELECT * FROM pending_orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Long): PendingOrderEntity?

    @Query("SELECT * FROM pending_orders WHERE is_synced = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getLastPendingOrder(): PendingOrderEntity?
    
    @Query("SELECT * FROM pending_orders ORDER BY id DESC LIMIT 1")
    suspend fun getLastPendingOrderRegardlessOfSync(): PendingOrderEntity?

    @Update
    suspend fun updatePendingOrder(order: PendingOrderEntity)

    @Delete
    suspend fun deletePendingOrder(order: PendingOrderEntity)

}

