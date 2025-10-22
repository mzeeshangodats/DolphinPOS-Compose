package com.retail.dolphinpos.domain.model.auth.login.response

data class LoginData(
    val accessToken: String,
    val expiresIn: String,
    val refreshToken: String,
    val user: User,
    val allStoreUsers: List<AllStoreUsers>,
    val storeInfo: Store
)