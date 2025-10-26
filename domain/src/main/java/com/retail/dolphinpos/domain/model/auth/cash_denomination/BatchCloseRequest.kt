package com.retail.dolphinpos.domain.model.auth.cash_denomination

data class BatchCloseRequest(
    val batchId: Int,
    val closingCashAmount: Double
)

