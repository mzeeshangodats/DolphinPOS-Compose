package com.retail.dolphinpos.domain.model.refund

data class CreateRefundRequest(
    val orderId: Int, // Server order ID
    val refundType: String, // "FULL" or "PARTIAL"
    val refundAmount: Double,
    val refundedItems: List<RefundedItemRequest>? = null,
    val paymentMethod: String,
    val reason: String? = null
)

