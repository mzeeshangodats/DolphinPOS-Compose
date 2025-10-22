package com.retail.dolphinpos.domain.model.auth.batch

data class Batch(
    val batchNo: String,
    val userId: Int?,
    val storeId: Int?,
    val registerId: Int?,
    val startingCashAmount: Double
)
