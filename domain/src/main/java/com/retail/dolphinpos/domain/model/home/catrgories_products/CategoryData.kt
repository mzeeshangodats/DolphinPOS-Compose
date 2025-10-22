package com.retail.dolphinpos.domain.model.home.catrgories_products

data class CategoryData(
    val description: String,
    val id: Int,
    val productCount: Int,
    val products: List<Products>?,
    val title: String
)