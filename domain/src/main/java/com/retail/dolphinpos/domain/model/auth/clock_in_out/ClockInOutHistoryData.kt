package com.retail.dolphinpos.domain.model.auth.clock_in_out

data class ClockInOutHistoryData(
    val check_in_id: Int,
    val check_in_time: String,
    val check_out_id: Int,
    val check_out_time: String,
    val userId: Int
)