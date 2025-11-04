package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

interface CancelTransactionUseCase {
    suspend operator fun invoke(
        sessionId: String?
    )
}