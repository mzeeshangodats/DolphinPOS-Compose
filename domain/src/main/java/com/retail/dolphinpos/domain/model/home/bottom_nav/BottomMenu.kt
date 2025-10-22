package com.retail.dolphinpos.domain.model.home.bottom_nav

import androidx.annotation.IdRes

data class BottomMenu(
    val menuName: String,
    @IdRes val destinationId: Int
)
