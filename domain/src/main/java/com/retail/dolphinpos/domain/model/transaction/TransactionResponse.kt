package com.retail.dolphinpos.domain.model.transaction

import com.google.gson.annotations.SerializedName
import com.retail.dolphinpos.domain.model.TaxDetail

data class TransactionResponse(
    val data: TransactionData?
)

data class TransactionData(
    @SerializedName("totalRecords")
    val totalRecords: Int,
    val list: List<TransactionItem>
)

data class TransactionItem(
    val id: Int,
    @SerializedName("orderId")
    val orderId: Int?,
    @SerializedName("storeId")
    val storeId: Int?,
    @SerializedName("paymentMethod")
    val paymentMethod: String,
    val status: String,
    val amount: String, // API returns as string
    @SerializedName("invoiceNo")
    val invoiceNo: String?,
    @SerializedName("batchId")
    val batchId: Int?,
    @SerializedName("userId")
    val userId: Int?,
    @SerializedName("orderSource")
    val orderSource: String?,
    val tax: Double?,
    val tip: Double?,
    @SerializedName("cardDetails")
    val cardDetails: Any?, // Can be String, Object, or null
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String?,
    val order: TransactionOrder?,
    val batch: TransactionBatch?
)

data class TransactionOrder(
    @SerializedName("orderNumber")
    val orderNumber: String?,
    val source: String?,
    @SerializedName("taxDetails")
    val taxDetails: List<TaxDetail>? = null,  // Store-level tax breakdown
    @SerializedName("orderItems")
    val orderItems: List<TransactionOrderItem>? = null  // Order items with tax details
)

data class TransactionOrderItem(
    @SerializedName("totalTax")
    val totalTax: Double? = null,  // Product-level tax amount
    @SerializedName("appliedTaxes")
    val appliedTaxes: List<TaxDetail>? = null  // Product-level tax breakdown
)

data class TransactionBatch(
    val id: Int?,
    @SerializedName("batchNo")
    val batchNo: String?
)
