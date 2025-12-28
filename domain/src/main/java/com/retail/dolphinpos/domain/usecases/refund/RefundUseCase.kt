package com.retail.dolphinpos.domain.usecases.refund

import android.content.Context
import com.retail.dolphinpos.domain.model.order.OrderRefundStatus
import com.retail.dolphinpos.domain.model.refund.Refund
import com.retail.dolphinpos.domain.model.refund.RefundRequest
import com.retail.dolphinpos.domain.repositories.refund.RefundRepository
import com.retail.dolphinpos.domain.usecases.sync.ScheduleSyncUseCase
import javax.inject.Inject

class RefundUseCase @Inject constructor(
    private val refundRepository: RefundRepository,
    private val validateRefundUseCase: ValidateRefundUseCase,
    private val calculateRefundAmountUseCase: CalculateRefundAmountUseCase,
    private val scheduleSyncUseCase: ScheduleSyncUseCase
) {
    
    suspend fun executeRefund(request: RefundRequest, context: Context): Result<Refund> {
        return try {
            // Step 1: Validate refund request
            validateRefundUseCase.validateRefund(request).getOrThrow()
            
            // Step 2: Get order data
            val order = refundRepository.getOrderById(request.orderId)
                ?: return Result.failure(IllegalArgumentException("Order not found"))
            
            // Step 3: Calculate refund amount
            val (refundAmount, refundedItems) = calculateRefundAmountUseCase.calculateRefundAmount(request, order).getOrThrow()
            
            // Step 4: Create refund with calculated amount
            val refundResult = refundRepository.createRefund(request, refundAmount, refundedItems)
            val refund = refundResult.getOrThrow()
            
            // Step 5: Update order refund status and amount
            val totalRefunded = refundRepository.getTotalRefundedAmountForOrder(request.orderId)
            val newRefundStatus = when {
                totalRefunded >= order.total -> OrderRefundStatus.FULL
                totalRefunded > 0 -> OrderRefundStatus.PARTIAL
                else -> OrderRefundStatus.NONE
            }
            refundRepository.updateOrderRefundStatus(request.orderId, totalRefunded, newRefundStatus)
            
            // Step 6: Restore inventory for refunded items
            for (item in refundedItems) {
                refundRepository.restoreInventory(item.productId, item.productVariantId, item.quantity)
            }
            
            // Step 7: Schedule sync if offline
            scheduleSyncUseCase.scheduleSync(context)
            
            Result.success(refund)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

