package com.retail.dolphinpos.domain.model.home.create_order

data class CardDetails(
    val paxBatchNo: String? = null,
    val transactionNo: String? = null,
    val lastFourDigits: String? = null,
    val originalReferenceNo: String? = null,
    val ecrReference: String? = null,
    val edcType: String? = null,
    val authCode: String? = null
)
