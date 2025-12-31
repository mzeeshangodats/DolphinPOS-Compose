package com.retail.dolphinpos.domain.model.home.catrgories_products

import com.retail.dolphinpos.domain.model.home.create_order.TaxDetail

data class Variant(
    val attributes: Attributes?,
    val barCode: String? = "",
    val cardPrice: String? = "",
    val cashPrice: String? = "",
    val costPrice: String? = null,
    val id: Int,
    val images: List<VariantImage>,
    val price: String?,
    val quantity: Int,
    val sku: String?,
    val plu: String? = null,
    val title: String?,
    val taxAmount: Double? = null,
    val taxDetails: List<TaxDetail>? = null
)