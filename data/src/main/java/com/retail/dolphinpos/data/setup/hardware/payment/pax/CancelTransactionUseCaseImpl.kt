package com.retail.dolphinpos.data.setup.hardware.payment.pax

import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.CancelTransactionUseCase
import javax.inject.Inject

class CancelTransactionUseCaseImpl @Inject constructor(
    private val paxManager: PaxManager,
    private val sessionManager: TerminalSessionManager
) : CancelTransactionUseCase {

    override suspend operator fun invoke(
        sessionId: String?
    ) {
        val terminal = sessionManager.getTerminal(sessionId)
        paxManager.cancelTransactionBeforeApproved(terminal)
    }
}
