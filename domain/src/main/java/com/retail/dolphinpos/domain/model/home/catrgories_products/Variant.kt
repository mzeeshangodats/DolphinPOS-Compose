package com.retail.dolphinpos.domain.model.home.catrgories_products

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
    val title: String?
)