package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

data class CloseBatchResult(
    val isSuccess: Boolean,
    val message: String
)

interface CloseBatchUseCase {
    suspend operator fun invoke(
        sessionId: String?,
        onResult: (CloseBatchResult) -> Unit
    )
}

