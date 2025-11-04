package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

import javax.inject.Inject

class InitializeTerminalUseCase @Inject constructor(
    private val paxManager: PaxManager
) {
    operator fun invoke(callback: (Boolean, String, Terminal?) -> Unit) {
        paxManager.initTerminal(callback)
    }
}


