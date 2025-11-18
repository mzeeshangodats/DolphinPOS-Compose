package com.retail.dolphinpos.domain.model.home.create_order

import com.retail.dolphinpos.domain.model.TaxDetail

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
    val discountedAmount: Double?,
    val fixedDiscount: Double?,
    val discountReason: String?,
    val fixedPercentageDiscount: Double?,
    val discountType: String?,
    val cardPrice: Double?,
    // Tax-related fields for product-level tax
    val totalTax: Double? = null,  // Product-level tax amount for this item
    val appliedTaxes: List<TaxDetail>? = null  // Tax breakdown for this item (store + product taxes)
)
