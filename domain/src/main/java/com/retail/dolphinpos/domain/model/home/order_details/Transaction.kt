package com.retail.dolphinpos.domain.model.home.order_details

import com.retail.dolphinpos.domain.model.home.create_order.CardDetails

data class Transaction(
    val amount: String,
    val batch: Any,
    val batchId: Any,
    val cardDetails: CardDetails,
    val createdAt: String,
    val id: Int,
    val invoiceNo: String,
    val locationId: Int,
    val orderId: Int,
    val orderSource: String,
    val paymentMethod: String,
    val status: String,
    val storeId: Int,
    val tax: Double,
    val tip: Any,
    val updatedAt: String,
    val userId: Int
)