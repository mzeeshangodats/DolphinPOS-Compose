package com.retail.dolphinpos.domain.model.refund

data class RefundRequest(
    val orderId: Long,
    val refundType: RefundType,
    val refundedItems: List<RefundedItemRequest>? = null, // For partial refunds
    val customAmount: Double? = null, // For custom partial refund amount
    val paymentMethod: String,
    val reason: String? = null
)

data class RefundedItemRequest(
    val productId: Int?,
    val productVariantId: Int?,
    val quantity: Int
)

