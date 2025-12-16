package com.retail.dolphinpos.domain.model.home.create_order

data class TaxDetail(
    val type: String?,
    val title: String?,
    val value: Double,
    val amount: Double? = null,
    val isDefault: Boolean? = false,
    val refundedTax: Double? = null
)