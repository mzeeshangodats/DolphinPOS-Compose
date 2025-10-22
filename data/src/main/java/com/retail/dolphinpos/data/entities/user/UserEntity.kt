package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val name: String?,
    val email: String?,
    val username: String?,
    val password: String = "",
    val pin: String?,
    val status: String?,
    val phoneNo: String?,
    val storeId: Int,
    val locationId: Int,
    val roleId: Int,
    val roleTitle: String?,
)