package com.retail.dolphinpos.data.entities.products

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "variants")
data class VariantsEntity(
    @PrimaryKey
    val id: Int,
    val productId: Int,
    val cardPrice: String?,
    val cashPrice: String?,
    val quantity: Int,
    val sku: String?,
    val plu: String?,
    val title: String?
)
