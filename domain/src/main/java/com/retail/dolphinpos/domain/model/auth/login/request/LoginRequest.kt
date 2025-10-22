package com.retail.dolphinpos.domain.model.auth.login.request

data class LoginRequest(
    val username: String,
    val password: String,
    val platform : String = "pos"
)