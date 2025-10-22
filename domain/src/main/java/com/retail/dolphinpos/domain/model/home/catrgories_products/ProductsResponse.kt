package com.retail.dolphinpos.domain.model.home.catrgories_products

import com.google.gson.annotations.SerializedName

data class ProductsResponse(
    @SerializedName("data")
    val category: Category?,
    val message: String,
    val metadata: Metadata,
    val success: Boolean
)