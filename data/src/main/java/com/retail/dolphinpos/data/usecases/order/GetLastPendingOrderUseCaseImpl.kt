package com.retail.dolphinpos.data.usecases.order

import com.retail.dolphinpos.data.repositories.pending_order.PendingOrderRepositoryImpl
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.domain.usecases.order.GetLastPendingOrderUseCase
import javax.inject.Inject

class GetLastPendingOrderUseCaseImpl @Inject constructor(
    private val pendingOrderRepository: PendingOrderRepositoryImpl
) : GetLastPendingOrderUseCase {

    override suspend operator fun invoke(): PendingOrder? {
        return pendingOrderRepository.getLastPendingOrder()?.let { entity ->
            pendingOrderRepository.convertToPendingOrder(entity)
        }
    }
}

