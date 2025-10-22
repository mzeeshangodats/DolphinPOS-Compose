package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_logo_url")

data class StoreLogoUrlEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val storeID: Int,
    val alt: String?,
    val original: String?,
    val thumbnail: String?
)