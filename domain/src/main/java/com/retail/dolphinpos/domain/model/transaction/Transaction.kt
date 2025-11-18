package com.retail.dolphinpos.domain.model.transaction

import com.retail.dolphinpos.domain.model.TaxDetail

data class Transaction(
    val id: Long = 0,
    val orderNo: String? = null,
    val orderId: Int? = null,
    val storeId: Int? = null,
    val locationId: Int? = null,
    val paymentMethod: String,
    val status: String,
    val amount: Double,
    val invoiceNo: String? = null,
    val batchId: Int? = null,
    val batchNo: String? = null,
    val userId: Int? = null,
    val orderSource: String? = null,
    val tax: Double? = null,
    val tip: Double? = null,
    val cardDetails: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Tax-related fields
    val taxDetails: List<TaxDetail>? = null  // Tax breakdown for this transaction
)

