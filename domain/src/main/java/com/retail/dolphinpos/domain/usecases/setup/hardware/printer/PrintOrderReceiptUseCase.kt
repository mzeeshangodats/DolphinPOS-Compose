package com.retail.dolphinpos.domain.usecases.setup.hardware.printer

import com.retail.dolphinpos.domain.model.order.PendingOrder

interface PrintOrderReceiptUseCase {
    suspend operator fun invoke(
        order: PendingOrder,
        statusCallback: (String) -> Unit = {}
    ): Result<Unit>
}


