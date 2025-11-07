package com.retail.dolphinpos.domain.usecases.order

import com.retail.dolphinpos.domain.model.order.PendingOrder

interface GetLatestOnlineOrderUseCase {
    suspend operator fun invoke(): PendingOrder?
}


