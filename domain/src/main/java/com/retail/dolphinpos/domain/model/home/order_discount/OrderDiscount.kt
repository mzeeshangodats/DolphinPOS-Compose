package com.retail.dolphinpos.domain.model.home.order_discount

import com.retail.dolphinpos.domain.model.home.cart.DiscountType

data class OrderDiscount(
    val reason: String,
    val type: DiscountType,
    val value: Double
)
