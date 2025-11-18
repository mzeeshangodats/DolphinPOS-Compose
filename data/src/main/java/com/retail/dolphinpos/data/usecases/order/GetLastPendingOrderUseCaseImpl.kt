package com.retail.dolphinpos.data.usecases.order

import com.retail.dolphinpos.data.repositories.order.OrderRepositoryImpl
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.domain.usecases.order.GetLastPendingOrderUseCase
import javax.inject.Inject

class GetLastPendingOrderUseCaseImpl @Inject constructor(
    private val orderRepository: OrderRepositoryImpl
) : GetLastPendingOrderUseCase {

    override suspend operator fun invoke(): PendingOrder? {
        // Get the last order regardless of sync status for printing purposes
        val latestOrder = orderRepository.getLatestOrder()
        return latestOrder?.let { entity ->
            orderRepository.convertToPendingOrder(entity)
        }
    }
}

