package com.retail.dolphinpos.domain.model.auth.cash_denomination

data class BatchCloseRequest(
    val cashierId: Int,
    val closedBy: Int,
    val closingCashAmount: Double,
    val locationId: Int,
    val orders: List<AbandonCart>,
    val paxBatchNo: String,
    val storeId: Int
)

data class AbandonCart(
    val subTotal : Double,
    val taxValue : Double,
    val total : Double,
    val items : List<AbandonCartItem>
)

data class AbandonCartItem(
    val productId : Int,
    val quantity : Int,
    val productVariantId : Int?
)