package com.retail.dolphinpos.data.entities.products

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_images")
data class ProductImagesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val fileURL: String?,
    val originalName: String?,
    val localPath: String? = null,
    val isCached: Boolean = false
)
