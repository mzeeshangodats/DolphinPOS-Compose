package com.retail.dolphinpos.domain.model.label

data class Label(
    val productName: String,
    val variantName: String = "",
    val barcode: String,
    val cashPrice: Double,
    val cardPrice: Double,
    val cashDiscountedPrice: Double = 0.0,
    val cardDiscountedPrice: Double = 0.0,
    val isDiscounted: Boolean = false,
    val applyDualPrice: Boolean = true,
    val printQuantity: Int = 1,
    )

