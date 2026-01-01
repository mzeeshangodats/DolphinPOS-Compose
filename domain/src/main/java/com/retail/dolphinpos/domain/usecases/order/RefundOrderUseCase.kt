package com.retail.dolphinpos.domain.usecases.order

import com.retail.dolphinpos.domain.model.home.refund.RefundRequest
import com.retail.dolphinpos.domain.model.home.refund.RefundResponse
import com.retail.dolphinpos.domain.repositories.home.OrdersRepository
import javax.inject.Inject

class RefundOrderUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository
) {
    suspend operator fun invoke(refundRequest: RefundRequest): Result<RefundResponse> {
        return ordersRepository.refundOrder(refundRequest)
    }
}

