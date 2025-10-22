package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_registers")
data class RegisterEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val name: String?,
    val status: String?,
    val locationId: Int
)