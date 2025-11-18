package com.retail.dolphinpos.data.usecases.order

import com.retail.dolphinpos.data.repositories.order.OrderRepositoryImpl
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.domain.usecases.order.GetLatestOnlineOrderUseCase
import javax.inject.Inject

class GetLatestOnlineOrderUseCaseImpl @Inject constructor(
    private val orderRepository: OrderRepositoryImpl
) : GetLatestOnlineOrderUseCase {

    override suspend operator fun invoke(): PendingOrder? {
        // Get latest synced orders (isSynced = true)
        val syncedOrders = orderRepository.getOrdersBySyncStatus(isSynced = true)
        // Return the latest one (first in the list since they're ordered by created_at DESC)
        return syncedOrders.firstOrNull()?.let { order ->
            orderRepository.convertToPendingOrder(order)
        }
    }
}


