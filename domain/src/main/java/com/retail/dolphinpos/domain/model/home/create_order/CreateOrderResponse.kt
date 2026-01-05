package com.retail.dolphinpos.domain.model.home.create_order

import com.google.gson.annotations.SerializedName

data class CreateOrderResponse(
    val message: String,
    @SerializedName("orderId")
    val orderId: Int? = null,
    val id: Int? = null  // Alternative field name if API uses "id" instead of "orderId"
)
