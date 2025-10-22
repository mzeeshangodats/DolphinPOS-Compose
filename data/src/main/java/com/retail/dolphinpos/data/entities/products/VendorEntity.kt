package com.retail.dolphinpos.data.entities.products

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vendor")
data class VendorEntity(
    @PrimaryKey
    val id: Int,
    val productId: Int,
    val title: String
)
