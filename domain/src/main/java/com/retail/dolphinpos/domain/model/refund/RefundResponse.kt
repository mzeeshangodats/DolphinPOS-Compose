package com.retail.dolphinpos.domain.model.refund

data class RefundResponse(
    val success: Boolean,
    val message: String,
    val refund: Refund? = null,
    val errors: Map<String, String>? = null
)

