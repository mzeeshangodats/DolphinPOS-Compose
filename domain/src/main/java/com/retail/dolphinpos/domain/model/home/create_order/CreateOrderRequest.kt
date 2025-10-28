package com.retail.dolphinpos.domain.model.home.create_order

data class CreateOrderRequest(
    val orderNo: String?,
    val customerId: Int?,
    val storeId: Int,
    val locationId: Int,
    val storeRegisterId: Int? = null,
    val paymentMethod: String,
    val isRedeemed: Boolean = false,
    val source: String = "point-of-sale",
    val redeemPoints: Int? = null,
    val items: List<CheckOutOrderItem>,
    val subTotal: Double,
    val total: Double,
    val applyTax: Boolean = true,
    val taxValue: Double,
    val discountAmount: Double,
    val cashDiscountAmount: Double,
    val rewardDiscount: Double,
    val discountIds: List<Int>? = null,
    val transactionId: String? = null,
    val batchId: Int = 0,
    val cashierId: Int = 0,
    val voidReason: String? = null,
    val isVoid: Boolean = false,
    val transactions: List<CheckoutSplitPaymentTransactions>? = null,
    val cardDetails: CardDetails? = null
)