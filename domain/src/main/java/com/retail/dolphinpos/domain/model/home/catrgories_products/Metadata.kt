package com.retail.dolphinpos.domain.model.home.catrgories_products

data class Metadata(
    val lastSync: Any,
    val locationId: String,
    val storeId: String,
    val syncTimestamp: String,
    val totalCategories: Int,
    val totalProducts: Int
)