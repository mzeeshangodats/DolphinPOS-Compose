package com.retail.dolphinpos.domain.usecases.tax

data class DiscountOrder(
    val discountType: String?,
    val percentage: Double,
    val amount: Double
)

