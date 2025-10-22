package com.retail.dolphinpos.domain.model.auth.cash_denomination

data class BatchOpenRequest(
    val storeId : Int,
    val cashierId : Int,
    val storeRegisterId : Int? = null,
    val startingCashAmount : Double
)