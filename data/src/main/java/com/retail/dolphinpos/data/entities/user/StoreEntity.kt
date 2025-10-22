package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store")

data class StoreEntity(
    @PrimaryKey val id: Int,
    val userID: Int,
    val name: String?,
    val address: String?,
    val multiCashier: Boolean?,
    val policy: String?,
    val advertisementImg: String?,
    val isAdvertisement: Int?
)
