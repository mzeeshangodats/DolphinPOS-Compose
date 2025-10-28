package com.retail.dolphinpos.domain.model.auth.cash_denomination

data class BatchOpenRequest(
    val batchNo: String,
    val storeId: Int,
    val userId: Int,
    val locationId: Int,
    val storeRegisterId: Int? = null,
    val startingCashAmount: Double
)