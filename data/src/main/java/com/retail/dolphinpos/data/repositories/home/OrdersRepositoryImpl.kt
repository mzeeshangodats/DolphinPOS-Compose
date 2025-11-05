package com.retail.dolphinpos.data.repositories.home

import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailsResponse
import com.retail.dolphinpos.domain.repositories.home.OrdersRepository

class OrdersRepositoryImpl(
    private val api: ApiService
) : OrdersRepository {

    override suspend fun getOrdersDetails(
        orderBy: String,
        order: String,
        startDate: String,
        endDate: String,
        limit: Int,
        page: Int,
        paginate: Boolean,
        storeId: Int,
        keyword: String?
    ): Result<OrderDetailsResponse> {
        return safeApiCallResult(
            apiCall = {
                api.getOrdersDetails(
                    orderBy = orderBy,
                    order = order,
                    startDate = startDate,
                    endDate = endDate,
                    limit = limit,
                    page = page,
                    paginate = paginate,
                    storeId = storeId,
                    keyword = keyword
                )
            },
            defaultMessage = "Failed to load orders"
        )
    }
}

