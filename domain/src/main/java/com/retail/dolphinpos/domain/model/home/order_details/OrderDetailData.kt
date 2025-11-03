package com.retail.dolphinpos.domain.model.home.order_details

data class OrderDetailData(
    val list: List<OrderDetailList>,
    val totalRecords: Int
)