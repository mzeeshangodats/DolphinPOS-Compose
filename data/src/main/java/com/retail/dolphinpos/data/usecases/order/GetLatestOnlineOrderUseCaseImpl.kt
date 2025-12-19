package com.retail.dolphinpos.data.usecases.order

import com.retail.dolphinpos.data.repositories.order.OrderRepositoryImpl
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.domain.usecases.order.GetLatestOnlineOrderUseCase
import javax.inject.Inject

class GetLatestOnlineOrderUseCaseImpl @Inject constructor(
    private val orderRepository: OrderRepositoryImpl
) : GetLatestOnlineOrderUseCase {

    override suspend operator fun invoke(): PendingOrder? {
        // Get latest order regardless of sync status
        val latestOrder = orderRepository.getLatestOrder()
        return latestOrder?.let { order ->
            orderRepository.convertToPendingOrder(order)
        }
    }
}


