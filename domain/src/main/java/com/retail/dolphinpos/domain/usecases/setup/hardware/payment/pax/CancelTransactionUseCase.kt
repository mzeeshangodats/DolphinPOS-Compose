package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

import javax.inject.Inject

class CancelTransactionUseCase @Inject constructor(
    private val paxManager: PaxManager
) {

    operator fun invoke(
        terminal: Terminal
    ) {
        paxManager.cancelTransactionBeforeApproved(terminal)
    }

}