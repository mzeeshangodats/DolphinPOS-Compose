package com.retail.dolphinpos.domain.model.home.order_details

data class CardDetails(
    val authCode: String,
    val brand: String,
    val entryMethod: String,
    val last4: String,
    val merchantId: String,
    val rrn: String,
    val terminalId: String,
    val terminalInvoiceNo: String,
    val transactionId: String
)