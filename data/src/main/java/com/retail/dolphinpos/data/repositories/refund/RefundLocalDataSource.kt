package com.retail.dolphinpos.data.repositories.refund

import com.retail.dolphinpos.data.dao.RefundDao
import com.retail.dolphinpos.data.entities.refund.RefundEntity
import com.retail.dolphinpos.data.entities.refund.RefundStatus
import javax.inject.Inject

class RefundLocalDataSource @Inject constructor(
    private val refundDao: RefundDao
) {
    suspend fun insertRefund(refund: RefundEntity): Long {
        return refundDao.insertRefund(refund)
    }
    
    suspend fun getRefundById(refundId: Long): RefundEntity? {
        return refundDao.getRefundById(refundId)
    }
    
    suspend fun getRefundByRefundId(refundId: String): RefundEntity? {
        return refundDao.getRefundByRefundId(refundId)
    }
    
    suspend fun getRefundsByOrderId(orderId: Long): List<RefundEntity> {
        return refundDao.getRefundsByOrderId(orderId)
    }
    
    suspend fun getPendingRefunds(): List<RefundEntity> {
        return refundDao.getPendingRefunds()
    }
    
    suspend fun updateRefund(refund: RefundEntity) {
        refundDao.updateRefund(refund)
    }
    
    suspend fun markRefundAsSynced(refundId: String, serverId: Int) {
        refundDao.markRefundAsSynced(refundId, RefundStatus.SYNCED, serverId, System.currentTimeMillis())
    }
    
    suspend fun getTotalRefundedAmountForOrder(orderId: Long): Double {
        return refundDao.getTotalRefundedAmountForOrder(orderId) ?: 0.0
    }
}

