package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val storeID: Int,
    val name: String?,
    val address: String?,
    val status: String?,
    val zipCode: String?,
    val taxValue: String?,
    val taxTitle: String?,
    val startTime: String?,
    val endTime: String?,
    val multiCashier: Boolean?
)