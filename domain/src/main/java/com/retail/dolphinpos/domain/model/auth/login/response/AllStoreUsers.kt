package com.retail.dolphinpos.domain.model.auth.login.response

data class AllStoreUsers(
    val id: Int = 0,
    val name: String? = "",
    val email: String? = "",
    val username: String? = "",
    val password: String? = "",
    val pin: String? = "",
    val status: String? = "",
    val phoneNo: String? = "",
    val storeId: Int = 0,
    val locationId: Int = 0,
    val roleId: Int = 0,
    val roleTitle: String? = "",
)