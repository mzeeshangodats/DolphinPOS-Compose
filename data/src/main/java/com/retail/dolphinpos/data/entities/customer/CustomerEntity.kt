package com.retail.dolphinpos.data.entities.customer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val storeId: Int,
    val locationId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val birthday: String,
    val createdAt: Long,
    val updatedAt: String
)
