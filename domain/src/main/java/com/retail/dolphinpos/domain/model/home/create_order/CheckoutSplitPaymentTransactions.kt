package com.retail.dolphinpos.domain.model.home.create_order

data class CheckoutSplitPaymentTransactions(
    val paymentMethod: String,
    val amount: Double,
    val cardDetails: CardDetails? = null,
    val baseAmount: Double? = null,
    val taxAmount: Double? = null,
    val dualPriceAmount: Double? = null
)