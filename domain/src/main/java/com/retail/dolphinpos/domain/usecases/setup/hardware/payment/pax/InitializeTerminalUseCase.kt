package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

data class InitializeTerminalResult(
    val isSuccess: Boolean,
    val message: String,
    val session: TerminalSession? = null
)

interface InitializeTerminalUseCase {
    suspend operator fun invoke(
        onResult: (InitializeTerminalResult) -> Unit
    )
}


