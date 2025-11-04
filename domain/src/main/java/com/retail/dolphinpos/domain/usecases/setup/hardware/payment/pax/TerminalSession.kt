package com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax

/**
 * Domain model representing a terminal session.
 * This abstraction allows the domain layer to work with terminals without
 * depending on PAX SDK types.
 */
data class TerminalSession(
    val sessionId: String,
    val appName: String
)
