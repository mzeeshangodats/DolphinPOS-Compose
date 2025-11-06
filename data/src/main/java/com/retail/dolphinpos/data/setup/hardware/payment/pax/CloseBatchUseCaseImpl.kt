package com.retail.dolphinpos.data.setup.hardware.payment.pax

import android.os.Bundle
import android.util.Log
import com.retail.dolphinpos.domain.analytics.AnalyticsTracker
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.CloseBatchResult
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.CloseBatchUseCase
import javax.inject.Inject

class CloseBatchUseCaseImpl @Inject constructor(
    private val paxManager: PaxManager,
    private val terminalSessionManager: TerminalSessionManager,
    private val analyticsTracker: AnalyticsTracker
) : CloseBatchUseCase {

    companion object {
        private const val TAG = "CloseBatchUseCaseImpl"
    }

    override suspend operator fun invoke(
        sessionId: String?,
        onResult: (CloseBatchResult) -> Unit
    ) {
        Log.d(TAG, "invoke: Starting batch close, sessionId: $sessionId")
        
        // Get terminal from session
        val terminal = sessionId?.let { id ->
            terminalSessionManager.getTerminal(id)
        }

        if (terminal == null) {
            Log.w(TAG, "invoke: Terminal not found for sessionId: $sessionId")
            onResult(
                CloseBatchResult(
                    isSuccess = false,
                    message = "Terminal not found. Please initialize terminal first."
                )
            )
            return
        }

        Log.d(TAG, "invoke: Terminal found, calling requestPaxBatchClose")
        paxManager.requestPaxBatchClose(
            terminal = terminal,
            onSuccess = { paxBatchCloseResponse ->
                // Map PAX SDK response to domain result
                // PAX SDK BatchCloseResponse has responseMessage() method
                val message = paxBatchCloseResponse.responseMessage() ?: "Batch closed successfully"
                Log.d(TAG, "invoke: Batch closed successfully - $message")
                analyticsTracker.logEvent("pax_batch_close_success", Bundle().apply {
                    putString("message", message)
                })
                onResult(
                    CloseBatchResult(
                        isSuccess = true,
                        message = message
                    )
                )
            },
            onFailure = { errorMessage ->
                Log.e(TAG, "invoke: Batch close failed - $errorMessage")
                analyticsTracker.logEvent("pax_batch_close_failure", Bundle().apply {
                    putString("error", errorMessage)
                })
                onResult(
                    CloseBatchResult(
                        isSuccess = false,
                        message = errorMessage
                    )
                )
            }
        )
    }
}

