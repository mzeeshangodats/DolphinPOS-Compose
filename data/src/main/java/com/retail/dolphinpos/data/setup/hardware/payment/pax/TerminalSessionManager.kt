package com.retail.dolphinpos.data.setup.hardware.payment.pax

import com.pax.poslinksemiintegration.Terminal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalSessionManager @Inject constructor() {
    private val sessions = mutableMapOf<String, Terminal>()

    fun createSession(terminal: Terminal): String {
        val sessionId = UUID.randomUUID().toString()
        sessions[sessionId] = terminal
        return sessionId
    }

    fun getTerminal(sessionId: String?): Terminal? {
        return sessionId?.let { sessions[it] }
    }

    fun removeSession(sessionId: String?) {
        sessionId?.let { sessions.remove(it) }
    }
}
