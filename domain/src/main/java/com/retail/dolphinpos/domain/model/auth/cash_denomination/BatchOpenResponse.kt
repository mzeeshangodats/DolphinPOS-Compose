package com.retail.dolphinpos.domain.model.auth.cash_denomination

import com.retail.dolphinpos.domain.model.auth.batch.Batch

data class BatchOpenResponse(
    val data: Batch,
    val message: String?,
)

