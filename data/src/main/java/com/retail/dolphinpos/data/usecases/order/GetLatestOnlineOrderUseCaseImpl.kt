package com.retail.dolphinpos.data.usecases.order

import com.retail.dolphinpos.data.repositories.online_order.OnlineOrderRepository
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.domain.usecases.order.GetLatestOnlineOrderUseCase
import javax.inject.Inject

class GetLatestOnlineOrderUseCaseImpl @Inject constructor(
    private val onlineOrderRepository: OnlineOrderRepository
) : GetLatestOnlineOrderUseCase {

    override suspend operator fun invoke(): PendingOrder? {
        return onlineOrderRepository.getLatestOnlineOrder()?.let { entity ->
            onlineOrderRepository.convertToPendingOrder(entity)
        }
    }
}


