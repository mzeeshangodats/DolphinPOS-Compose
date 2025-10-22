package com.retail.dolphinpos.domain.model.auth.login.response

data class Store(
    val id: Int,
    val name: String?,
    val address: String?,
    val multiCashier: Boolean?,
    val policy: String?,
    val advertisementImg: String?,
    val isAdvertisement: Int?,
    val logoUrl: StoreLogoUrl?,
    val locations: List<Locations>?,
)