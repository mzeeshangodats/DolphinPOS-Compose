package com.retail.dolphinpos.domain.model.refund

data class Refund(
    val id: Long = 0,
    val refundId: String,
    val orderId: Long,
    val orderNo: String,
    val refundType: RefundType,
    val refundAmount: Double,
    val refundedItems: List<RefundedItem>,
    val paymentMethod: String,
    val refundStatus: RefundStatus,
    val serverId: Int? = null,
    val storeId: Int,
    val locationId: Int,
    val userId: Int,
    val batchNo: String? = null,
    val reason: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

