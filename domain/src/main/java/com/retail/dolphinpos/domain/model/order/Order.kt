package com.retail.dolphinpos.domain.model.order

import com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem

data class Order(
    val id: Long,
    val orderNumber: String,
    val orderItems: List<CheckOutOrderItem>,
    val subTotal: Double,
    val total: Double,
    val taxValue: Double,
    val discountAmount: Double,
    val cashDiscountAmount: Double,
    val rewardDiscount: Double,
    val totalRefundedAmount: Double = 0.0,
    val refundStatus: OrderRefundStatus = OrderRefundStatus.NONE,
    val isVoid: Boolean = false
)

