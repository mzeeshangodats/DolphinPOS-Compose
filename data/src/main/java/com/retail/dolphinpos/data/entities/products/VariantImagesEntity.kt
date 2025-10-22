package com.retail.dolphinpos.data.entities.products

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "variant_images")
data class VariantImagesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val variantId: Int,
    val fileURL: String?,
    val originalName: String?,
    val localPath: String? = null,
    val isCached: Boolean = false
)
