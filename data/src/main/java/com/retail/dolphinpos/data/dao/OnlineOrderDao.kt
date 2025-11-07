package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retail.dolphinpos.data.entities.order.OnlineOrderEntity

@Dao
interface OnlineOrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnlineOrder(order: OnlineOrderEntity): Long

    @Query("SELECT * FROM online_orders ORDER BY created_at DESC")
    suspend fun getAllOnlineOrders(): List<OnlineOrderEntity>

    @Query("SELECT * FROM online_orders ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestOnlineOrder(): OnlineOrderEntity?

    @Query("SELECT * FROM online_orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Long): OnlineOrderEntity?

    @Query("SELECT * FROM online_orders WHERE order_no = :orderNumber")
    suspend fun getOrderByOrderNumber(orderNumber: String): OnlineOrderEntity?

    @Query("SELECT * FROM online_orders WHERE store_id = :storeId ORDER BY created_at DESC")
    suspend fun getOrdersByStoreId(storeId: Int): List<OnlineOrderEntity>

    @Query("SELECT * FROM online_orders WHERE location_id = :locationId ORDER BY created_at DESC")
    suspend fun getOrdersByLocationId(locationId: Int): List<OnlineOrderEntity>

    @Query("SELECT * FROM online_orders WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getOrdersByUserId(userId: Int): List<OnlineOrderEntity>

    @Update
    suspend fun updateOnlineOrder(order: OnlineOrderEntity)

    @Delete
    suspend fun deleteOnlineOrder(order: OnlineOrderEntity)

    @Query("DELETE FROM online_orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Long)

}

