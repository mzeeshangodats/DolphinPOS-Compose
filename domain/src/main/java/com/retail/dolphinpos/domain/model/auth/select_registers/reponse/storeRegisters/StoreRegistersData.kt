package com.retail.dolphinpos.domain.model.auth.select_registers.reponse.storeRegisters

data class StoreRegistersData(
    val createdAt: String,
    val id: Int,
    val locationId: Int,
    val name: String,
    val status: String,
    val storeId: Int,
    val updatedAt: String
)