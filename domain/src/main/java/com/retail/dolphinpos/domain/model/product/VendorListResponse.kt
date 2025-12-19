package com.retail.dolphinpos.domain.model.product

data class VendorListResponse(
    val data: VendorListData
)

data class VendorListData(
    val totalRecords: Int,
    val list: List<VendorItem>
)

data class VendorItem(
    val id: Int,
    val title: String,
    val address: String? = null,
    val wpId: Int? = null,
    val storeId: Int? = null,
    val locationId: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

