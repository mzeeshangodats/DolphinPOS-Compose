package com.retail.dolphinpos.domain.model.home.order_details

data class Product(
    val id: Int,
    val images: List<Image>,
    val name: String
)