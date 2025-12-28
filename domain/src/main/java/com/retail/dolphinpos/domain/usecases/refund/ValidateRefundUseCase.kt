package com.retail.dolphinpos.domain.usecases.refund

import com.retail.dolphinpos.domain.model.refund.RefundRequest
import com.retail.dolphinpos.domain.repositories.refund.RefundRepository
import javax.inject.Inject

class ValidateRefundUseCase @Inject constructor(
    private val refundRepository: RefundRepository
) {
    
    suspend fun validateRefund(request: RefundRequest): Result<Unit> {
        return try {
            // Check if order exists
            val order = refundRepository.getOrderById(request.orderId)
                ?: return Result.failure(IllegalArgumentException("Order not found"))
            
            // Check if order is voided
            if (order.isVoid) {
                return Result.failure(IllegalStateException("Cannot refund a voided order"))
            }
            
            // Check if order is already fully refunded
            val totalRefunded = refundRepository.getTotalRefundedAmountForOrder(request.orderId)
            val remainingAmount = order.total - totalRefunded
            
            if (remainingAmount <= 0) {
                return Result.failure(IllegalStateException("Order is already fully refunded"))
            }
            
            // For full refund, check if any amount is already refunded
            if (request.refundType == com.retail.dolphinpos.domain.model.refund.RefundType.FULL && totalRefunded > 0) {
                return Result.failure(IllegalStateException("Cannot perform full refund on partially refunded order"))
            }
            
            // For partial refund, validate amount
            if (request.refundType == com.retail.dolphinpos.domain.model.refund.RefundType.PARTIAL) {
                if (request.customAmount != null) {
                    if (request.customAmount <= 0) {
                        return Result.failure(IllegalArgumentException("Refund amount must be greater than zero"))
                    }
                    if (request.customAmount > remainingAmount) {
                        return Result.failure(IllegalArgumentException("Refund amount (${request.customAmount}) exceeds remaining order amount ($remainingAmount)"))
                    }
                } else if (request.refundedItems == null || request.refundedItems.isEmpty()) {
                    return Result.failure(IllegalArgumentException("Partial refund requires refunded items or custom amount"))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

