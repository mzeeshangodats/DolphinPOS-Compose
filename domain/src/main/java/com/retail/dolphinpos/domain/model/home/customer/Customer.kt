package com.retail.dolphinpos.domain.model.home.customer

data class Customer(
    val agreedToMarketingEmails: Boolean,
    val agreedToMarketingSMS: Boolean,
    val birthMonth: String,
    val birthYear: String,
    val createdAt: String,
    val deletedAt: Any,
    val email: String,
    val firstName: String,
    val id: Int,
    val lastName: String,
    val phoneNumber: String,
    val pointsEarned: Int,
    val storeId: Int,
    val updatedAt: String
)