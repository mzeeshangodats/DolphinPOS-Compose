package com.retail.dolphinpos.domain.model.auth.select_registers.reponse

data class VerifyRegisterData(
    val locationId: Int,
    val name: String,
    val status: String,
    val storeId: Int,
    val storeRegisterId: Int,
    val updatedAt: String
)