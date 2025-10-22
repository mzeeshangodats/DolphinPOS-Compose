package com.retail.dolphinpos.domain.model.auth.login.response

data class Registers(
    val id: Int,
    val name: String?,
    val status: String?,
    val locationId: Int,
)