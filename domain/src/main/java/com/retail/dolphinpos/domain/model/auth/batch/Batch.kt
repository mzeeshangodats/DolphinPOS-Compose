package com.retail.dolphinpos.domain.model.auth.batch

data class Batch(
    val batchId: Int = 0,
    val batchNo: String,
    val userId: Int?,
    val storeId: Int?,
    val registerId: Int?,
    val locationId: Int?,
    val startingCashAmount: Double
)
