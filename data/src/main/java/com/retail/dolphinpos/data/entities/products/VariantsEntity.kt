package com.retail.dolphinpos.data.entities.products

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "variants")
data class VariantsEntity(
    @PrimaryKey
    val id: Int,
    val productId: Int,
    val price: String?,
    val quantity: Int,
    val sku: String?,
    val title: String?
)
