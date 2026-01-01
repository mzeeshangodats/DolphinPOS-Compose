package com.retail.dolphinpos.data.repositories.orders_details

import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailsResponse
import com.retail.dolphinpos.domain.model.home.refund.RefundRequest
import com.retail.dolphinpos.domain.model.home.refund.RefundResponse
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

    override suspend fun refundOrder(refundRequest: RefundRequest): Result<RefundResponse> {
        return safeApiCallResult(
            apiCall = {
                api.refundOrder(refundRequest)
            },
            defaultMessage = "Failed to process refund"
        )
    }
}