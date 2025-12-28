package com.retail.dolphinpos.domain.model.refund

data class CreateRefundResponse(
    val success: Boolean,
    val message: String,
    val refund: RefundData? = null,
    val errors: Map<String, String>? = null
)

data class RefundData(
    val id: Int,
    val refundId: String,
    val orderId: Int,
    val refundType: String,
    val refundAmount: Double,
    val refundedItems: List<RefundedItem>? = null,
    val paymentMethod: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

