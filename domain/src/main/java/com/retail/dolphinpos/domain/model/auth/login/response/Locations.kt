package com.retail.dolphinpos.domain.model.auth.login.response

import com.retail.dolphinpos.domain.model.TaxDetail

data class Locations(
    val id: Int,
    val name: String?,
    val address: String?,
    val status: String?,
    val zipCode: String?,
    val taxValue: String?,
    val taxTitle: String?,
    val taxDetails: List<TaxDetail>? = null,
    val startTime: String?,
    val endTime: String?,
    val multiCashier: Boolean?,
    val registers: List<Registers>?,
)