package com.retail.dolphinpos.data.entities.products

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductsEntity(
    @PrimaryKey
    val id: Int,
    val categoryId: Int,
    val storeId: Int,
    val name: String?,
    val description: String?,
    val quantity: Int,
    val status: String?,
    val cashPrice: String,
    val cardPrice: String,
    val barCode: String?,
    val plu: String?,
    val chargeTaxOnThisProduct: Boolean?,
    val locationId: Int,
    val cardTax: Double = 0.0,
    val cashTax: Double = 0.0
)
