package com.retail.dolphinpos.domain.usecases.order

import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailList
import com.retail.dolphinpos.domain.model.order.PendingOrder

interface GetPrintableOrderFromOrderDetailUseCase {
    operator fun invoke(orderDetail: OrderDetailList): PendingOrder
}


