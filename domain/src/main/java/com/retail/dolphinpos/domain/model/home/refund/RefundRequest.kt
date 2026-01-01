package com.retail.dolphinpos.domain.model.home.refund

data class RefundRequest(
    val orderNumber: String,
    val storeId: Int,
    val batchNo: String,
    val userId: Int,
    val transactions: List<RefundTransaction>,
    val items: List<RefundItem>,
    val restoreInventory: Boolean = false,
    val fullRefund: Boolean = false
)

data class RefundTransaction(
    val invoiceNo: String,
    val amount: Double,
    val tax: Double
)

data class RefundItem(
   // val id: Int,
    val productId: Int,
    val quantity: Int,
    val price: Double
)

