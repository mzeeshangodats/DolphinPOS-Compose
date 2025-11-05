package com.retail.dolphinpos.domain.model.auth.batch

import java.io.Serializable

data class BatchDetails(
    val batchId: Int? = null,
    val batchNo: String? = null,
) : Serializable

