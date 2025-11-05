package com.retail.dolphinpos.data.setup.hardware.payment.pax

import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.InitializeTerminalResult
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.InitializeTerminalUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.TerminalSession
import javax.inject.Inject

class InitializeTerminalUseCaseImpl @Inject constructor(
    private val paxManager: PaxManager,
    private val sessionManager: TerminalSessionManager
) : InitializeTerminalUseCase {

    override suspend operator fun invoke(
        onResult: (InitializeTerminalResult) -> Unit
    ) {
        paxManager.initTerminal { isSuccess, messageOrAppName, terminal ->
            if (isSuccess && terminal != null) {
                val sessionId = sessionManager.createSession(terminal)
                val session = TerminalSession(
                    sessionId = sessionId,
                    appName = messageOrAppName
                )
                onResult(
                    InitializeTerminalResult(
                        isSuccess = true,
                        message = "Terminal initialized successfully: $messageOrAppName",
                        session = session
                    )
                )
            } else {
                onResult(
                    InitializeTerminalResult(
                        isSuccess = false,
                        message = messageOrAppName,
                        session = null
                    )
                )
            }
        }
    }
}
