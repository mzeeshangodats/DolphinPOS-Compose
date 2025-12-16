package com.retail.dolphinpos.domain.model.home.create_order

import com.retail.dolphinpos.domain.model.home.create_order.TaxDetail

data class CreateOrderRequest(
    val orderNumber: String?,
    val invoiceNo: String? = null,
    val customerId: Int?,
    val storeId: Int,
    val locationId: Int,
    val storeRegisterId: Int? = null,
    val batchNo: String? = null,
    val paymentMethod: String,
    val isRedeemed: Boolean = false,
    val source: String = "point-of-sale",
    val redeemPoints: Int? = null,
    val items: List<CheckOutOrderItem>,
    val subTotal: Double,
    val total: Double,
    val applyTax: Boolean = true,
    val taxValue: Double,
    val discountAmount: Double = 0.0,
    val cashDiscountAmount: Double = 0.0,
    val rewardDiscount: Double = 0.0,
    val discountIds: List<Int>? = null,
    val transactionId: String? = null,
    val userId: Int = 0,
    val voidReason: String? = null,
    val isVoid: Boolean = false,
    val transactions: List<CheckoutSplitPaymentTransactions>? = null,
    val cardDetails: CardDetails? = null,
    // Tax-related fields
    val taxDetails: List<TaxDetail>? = null,  // Store-level default taxes breakdown
    val taxExempt: Boolean = false
)