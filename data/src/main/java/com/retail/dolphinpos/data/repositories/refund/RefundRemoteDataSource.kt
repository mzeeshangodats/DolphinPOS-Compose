package com.retail.dolphinpos.data.repositories.refund

import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCall
import com.retail.dolphinpos.domain.model.refund.CreateRefundRequest
import com.retail.dolphinpos.domain.model.refund.CreateRefundResponse
import javax.inject.Inject

class RefundRemoteDataSource @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun createRefund(request: CreateRefundRequest): CreateRefundResponse {
        return safeApiCall(
            apiCall = { apiService.createRefund(request) },
            defaultResponse = {
                CreateRefundResponse(
                    success = false,
                    message = "Refund creation failed",
                    refund = null,
                    errors = null
                )
            }
        )
    }
}

