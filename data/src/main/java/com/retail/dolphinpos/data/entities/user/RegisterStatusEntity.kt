package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "register_status_details")
data class RegisterStatusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val storeId: Int,
    val locationId: Int,
    val storeRegisterId: Int,
    val status: String,
    val updatedAt: String,
)
