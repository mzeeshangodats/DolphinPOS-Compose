package com.retail.dolphinpos.domain.model.home.order_details

data class OrderItem(
    val discountedPrice: String,
    val isDiscounted: Boolean,
    val price: String,
    val product: Product,
    val productVariant: Any,
    val quantity: Int,
    val refundPrice: Any,
    val refundQuantity: Any
)