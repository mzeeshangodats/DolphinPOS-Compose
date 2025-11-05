package com.retail.dolphinpos.domain.model.auth.select_registers.request

data class VerifyRegisterRequest(
    val locationId: Int,
    val storeId: Int,
    val storeRegisterId: Int
)