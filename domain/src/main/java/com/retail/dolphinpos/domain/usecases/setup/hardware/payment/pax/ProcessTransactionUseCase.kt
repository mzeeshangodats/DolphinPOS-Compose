package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

import javax.inject.Inject

class ProcessTransactionUseCase @Inject constructor(
    private val paxManager: PaxManager
) {
    operator fun invoke(
        terminal: Terminal,
        amount: String,
        onSuccess: (DoCreditResponse) -> Unit,
        onFailure: (String) -> Unit
    ) {
        paxManager.startTransaction(terminal, amount, onSuccess, onFailure)
    }
}