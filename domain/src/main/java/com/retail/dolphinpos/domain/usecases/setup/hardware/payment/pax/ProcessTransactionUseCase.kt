package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

import com.retail.dolphinpos.domain.model.home.create_order.CardDetails

data class ProcessTransactionResult(
    val isSuccess: Boolean,
    val message: String,
    val cardDetails: CardDetails? = null
)

interface ProcessTransactionUseCase {
    suspend operator fun invoke(
        sessionId: String,
        amount: String,
        onResult: (ProcessTransactionResult) -> Unit
    )
}