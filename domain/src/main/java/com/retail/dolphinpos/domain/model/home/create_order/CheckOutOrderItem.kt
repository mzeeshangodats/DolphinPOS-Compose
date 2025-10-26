package com.retail.dolphinpos.domain.model.home.create_order

data class CheckOutOrderItem(
    val productId: Int?,
    val quantity: Int?,
    val productVariantId: Int?,
    val name: String?,
    val isCustom: Boolean?,
    val price: Double?,
    val barCode: String?,
    val reason: String?,
    val discountId: Int?,
    val discountedPrice: Double?,
    val fixedDiscount: Double?,
    val discountReason: String?,
    val fixedPercentageDiscount: Double?,
    val discountType: String?,
    val cardPrice: Double?
)
