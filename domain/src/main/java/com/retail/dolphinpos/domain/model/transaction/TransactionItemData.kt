package com.retail.dolphinpos.domain.model.transaction

data class TransactionItemData(
    val amount: String,
    val batch: Batch,
    val batchId: Int,
    val cardDetails: Any,
    val createdAt: String,
    val id: Int,
    val invoiceNo: String,
    val order: Order,
    val orderId: Int,
    val orderSource: String,
    val paymentMethod: String,
    val status: String,
    val storeId: Int,
    val tax: Any,
    val tip: Any,
    val updatedAt: String,
    val userId: Int
)