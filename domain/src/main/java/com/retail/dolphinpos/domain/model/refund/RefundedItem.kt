package com.retail.dolphinpos.domain.model.refund

data class RefundedItem(
    val productId: Int?,
    val productVariantId: Int?,
    val quantity: Int,
    val amount: Double, // Refunded amount for this item (including proportional discounts and taxes)
    val originalPrice: Double, // Original price per unit
    val refundedPrice: Double, // Refunded price per unit (after discounts)
    val itemDiscount: Double = 0.0, // Item-level discount refunded
    val itemTax: Double = 0.0, // Item-level tax refunded
    val orderDiscountProportion: Double = 0.0 // Proportion of order-level discount refunded
)

