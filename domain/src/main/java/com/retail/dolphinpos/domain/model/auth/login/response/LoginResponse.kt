package com.retail.dolphinpos.domain.model.auth.login.response

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("data")
    val loginData: LoginData?,
    val message: String?
)