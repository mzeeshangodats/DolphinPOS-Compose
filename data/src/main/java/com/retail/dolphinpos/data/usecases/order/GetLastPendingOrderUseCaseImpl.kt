package com.retail.dolphinpos.data.usecases.order

import com.retail.dolphinpos.data.entities.order.PendingOrderEntity
import com.retail.dolphinpos.data.repositories.order.PendingOrderRepository
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.domain.usecases.order.GetLastPendingOrderUseCase
import javax.inject.Inject

class GetLastPendingOrderUseCaseImpl @Inject constructor(
    private val pendingOrderRepository: PendingOrderRepository
) : GetLastPendingOrderUseCase {

    override suspend operator fun invoke(): PendingOrder? {
        val entity = pendingOrderRepository.getLastPendingOrder()
        return entity?.toDomain()
    }

    private fun PendingOrderEntity.toDomain(): PendingOrder {
        return pendingOrderRepository.convertToPendingOrder(this)
    }
}

