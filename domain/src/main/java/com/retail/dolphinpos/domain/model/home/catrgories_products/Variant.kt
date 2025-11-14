package com.retail.dolphinpos.domain.model.home.catrgories_products

import com.retail.dolphinpos.domain.model.TaxDetail

data class Variant(
    val attributes: Attributes?,
    val barCode: String? = "",
    val cardPrice: String? = "",
    val cashPrice: String? = "",
    val id: Int,
    val images: List<VariantImage>,
    val price: String?,
    val quantity: Int,
    val sku: String?,
    val title: String?,
    val taxAmount: Double? = null,
    val taxDetails: List<TaxDetail>? = null
)