package com.retail.dolphinpos.data.entities.products

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_images")
data class CachedImageEntity(
    @PrimaryKey
    val originalUrl: String,
    val localPath: String,
    val fileName: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0,
    val isDownloaded: Boolean = false
)
