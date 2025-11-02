package com.retail.dolphinpos.domain.model.auth.clock_in_out

data class ClockInOutRequest(
    val slug: String,
    val storeId: Int,
    val time: String,
    val userId: Int
)