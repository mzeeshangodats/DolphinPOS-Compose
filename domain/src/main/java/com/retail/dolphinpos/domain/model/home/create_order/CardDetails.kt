package com.retail.dolphinpos.domain.model.home.create_order

data class CardDetails(
    val terminalInvoiceNo: String? = null,
    val transactionId: String? = null,
    val authCode: String? = null,
    val rrn: String? = null,
    val brand: String? = null,
    val last4: String? = null,
    val entryMethod: String? = null,
    val merchantId: String? = null,
    val terminalId: String? = null,
    val paxBatchNo: String? = null,
    val originalReferenceNo: String? = null,
    val ecrReference: String? = null,
    val edcType: String? = null,
)
