package com.retail.dolphinpos.domain.model.auth.login.response

data class User(
    val id: Int = 0,
    val name: String? = "",
    val roleTitle: String? = "",
    val status: String? = "",
    val username: String? = "",
    val storeId: Int = 0,
    val locationId: Int = 0,
    val rootManagerId: Int? = 0,
    val pin: String? = ""
)