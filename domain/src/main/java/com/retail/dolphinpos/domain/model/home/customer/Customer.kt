package com.retail.dolphinpos.domain.model.home.customer

data class Customer(
    val id: Int = 0,
    val userId: Int,
    val storeId: Int,
    val locationId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val birthday: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: String = ""
)
