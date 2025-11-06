package com.retail.dolphinpos.domain.usecases.order

import com.retail.dolphinpos.domain.model.order.PendingOrder

interface GetLastPendingOrderUseCase {
    suspend operator fun invoke(): PendingOrder?
}

