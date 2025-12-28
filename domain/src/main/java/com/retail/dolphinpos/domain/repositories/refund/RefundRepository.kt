package com.retail.dolphinpos.domain.repositories.refund

import com.retail.dolphinpos.domain.model.refund.Refund
import com.retail.dolphinpos.domain.model.refund.RefundRequest
import com.retail.dolphinpos.domain.model.refund.RefundResponse
import com.retail.dolphinpos.domain.model.refund.RefundStatus

import com.retail.dolphinpos.domain.model.order.Order

interface RefundRepository {
    suspend fun createRefund(request: RefundRequest, refundAmount: Double, refundedItems: List<com.retail.dolphinpos.domain.model.refund.RefundedItem>): Result<Refund>
    suspend fun getRefundById(refundId: Long): Refund?
    suspend fun getRefundByRefundId(refundId: String): Refund?
    suspend fun getRefundsByOrderId(orderId: Long): List<Refund>
    suspend fun getPendingRefunds(): List<Refund>
    suspend fun syncRefundToServer(refund: Refund): Result<Refund>
    suspend fun updateRefundStatus(refundId: String, status: RefundStatus, serverId: Int? = null)
    suspend fun getTotalRefundedAmountForOrder(orderId: Long): Double
    suspend fun getOrderById(orderId: Long): Order?
    suspend fun updateOrderRefundStatus(orderId: Long, totalRefundedAmount: Double, refundStatus: com.retail.dolphinpos.domain.model.order.OrderRefundStatus)
    suspend fun restoreInventory(productId: Int?, productVariantId: Int?, quantity: Int)
}

