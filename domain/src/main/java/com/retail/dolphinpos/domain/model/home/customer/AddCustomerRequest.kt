package com.retail.dolphinpos.domain.model.home.customer

data class AddCustomerRequest(
    val birthMonth: String,
    val birthYear: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val storeId: Int
)