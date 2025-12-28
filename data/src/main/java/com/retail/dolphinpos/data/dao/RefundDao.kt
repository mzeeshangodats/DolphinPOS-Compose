package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retail.dolphinpos.data.entities.refund.RefundEntity
import com.retail.dolphinpos.data.entities.refund.RefundStatus

@Dao
interface RefundDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefund(refund: RefundEntity): Long
    
    @Query("SELECT * FROM refunds WHERE id = :refundId")
    suspend fun getRefundById(refundId: Long): RefundEntity?
    
    @Query("SELECT * FROM refunds WHERE refund_id = :refundId")
    suspend fun getRefundByRefundId(refundId: String): RefundEntity?
    
    @Query("SELECT * FROM refunds WHERE order_id = :orderId ORDER BY created_at DESC")
    suspend fun getRefundsByOrderId(orderId: Long): List<RefundEntity>
    
    @Query("SELECT * FROM refunds WHERE order_no = :orderNo ORDER BY created_at DESC")
    suspend fun getRefundsByOrderNo(orderNo: String): List<RefundEntity>
    
    @Query("SELECT * FROM refunds WHERE refund_status = :status ORDER BY created_at DESC")
    suspend fun getRefundsByStatus(status: RefundStatus): List<RefundEntity>
    
    @Query("SELECT * FROM refunds WHERE refund_status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingRefunds(): List<RefundEntity>
    
    @Query("SELECT * FROM refunds WHERE server_id = :serverId")
    suspend fun getRefundByServerId(serverId: Int): RefundEntity?
    
    @Update
    suspend fun updateRefund(refund: RefundEntity)
    
    @Query("UPDATE refunds SET refund_status = :status, server_id = :serverId, updated_at = :updatedAt WHERE refund_id = :refundId")
    suspend fun markRefundAsSynced(refundId: String, status: RefundStatus, serverId: Int, updatedAt: Long)
    
    @Query("SELECT SUM(refund_amount) FROM refunds WHERE order_id = :orderId")
    suspend fun getTotalRefundedAmountForOrder(orderId: Long): Double?
}

