package com.retail.dolphinpos.domain.repositories.home

import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailsResponse

interface OrdersRepository {
    suspend fun getOrdersDetails(
        orderBy: String = "createdAt",
        order: String = "desc",
        startDate: String,
        endDate: String,
        limit: Int,
        page: Int,
        paginate: Boolean = true,
        storeId: Int,
        keyword: String? = null
    ): Result<OrderDetailsResponse>
}

