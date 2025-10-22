package com.retail.dolphinpos.domain.model.auth.select_registers.reponse

data class UpdateStoreRegisterData(
    val storeId: Int,
    val locationId: Int,
    val storeRegisterId: Int,
    val status: String,
    val updatedAt: String,
)
