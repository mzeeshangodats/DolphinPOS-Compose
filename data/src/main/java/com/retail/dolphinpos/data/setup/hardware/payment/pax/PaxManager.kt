package com.retail.dolphinpos.data.setup.hardware.payment.pax

import android.app.Application
import android.os.Bundle
import android.util.Log
//import com.lingeriepos.common.module.AnalyticsModule.AnalyticsTracker
//import com.retail.dolphinpos.data.setup.hardware.payment.pax.ConnectionSetting.Http
//import com.retail.dolphinpos.data.setup.hardware.payment.pax.ConnectionSetting.Tcp
//import com.lingeriepos.common.usecases.batch.GetBatchDetailsUseCase
//import com.lingeriepos.common.usecases.pax.GetHttpSettingsUseCase
//import com.lingeriepos.common.usecases.pax.GetPaxDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.GetTcpSettingsUseCase
//import com.lingeriepos.common.usecases.user.GetUserDetailUseCase
//import com.lingeriepos.models.request.CardDetails
//import com.pax.poslinkadmin.Code100021
//import com.pax.poslinkadmin.constant.EdcType
//import com.pax.poslinkadmin.constant.TransactionType
//import com.pax.poslinkadmin.manage.GetVariableRequest
//import com.pax.poslinkadmin.manage.GetVariableResponse
//import com.pax.poslinkadmin.util.AmountRequest
//import com.pax.poslinksemiintegration.POSLinkSemi
//import com.pax.poslinksemiintegration.Terminal
//import com.pax.poslinksemiintegration.batch.BatchCloseRequest
//import com.pax.poslinksemiintegration.batch.BatchCloseResponse
//import com.pax.poslinksemiintegration.transaction.DoCreditRequest
//import com.pax.poslinksemiintegration.transaction.DoCreditResponse
//import com.pax.poslinksemiintegration.util.CashierRequest
//import com.pax.poslinksemiintegration.util.TraceRequest
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

const val CODE_SUCCESS = "000000"

@Singleton
class PaxManager @Inject constructor(
    private val application: Application,
    //private val getUserDetailsUseCase: GetUserDetailUseCase,
    //private val getBatchDetailsUseCase: GetBatchDetailsUseCase,
    //private val getPaxDetailsUseCase: GetPaxDetailsUseCase,
    private val getTcpSettingsUseCase: GetTcpSettingsUseCase,
    //private val getHttpSettingsUseCase: GetHttpSettingsUseCase,
    //private val analyticsTracker: AnalyticsTracker
) {
    companion object {
        const val TAG = "PaxManager"
    }

    private var paxRunningThread: Thread? = null

//    fun initTerminal(onResult: (Boolean, String, Terminal?) -> Unit) {
//        analyticsTracker.logEvent("pax_terminal_init_attempt", null)
//
//        val paxDetail = getPaxDetailsUseCase()
//        if (paxDetail == null) {
//            val error = "Pax details not found, Please proceed to More/Setup/Credit Card Processing"
//            analyticsTracker.logEvent("pax_terminal_init_failure", bundleOf("error" to error))
//            onResult(false, error, null)
//            return
//        }
//
//        val connectionSetting = if (paxDetail.isCommunicationTypeTCP()) {
//            Tcp(getTcpSettingsUseCase(paxDetail.ipAddress, paxDetail.portNumber))
//        } else {
//            Http(getHttpSettingsUseCase(paxDetail.ipAddress, paxDetail.portNumber))
//        }
//
//        runAction {
//            val terminal = when (connectionSetting) {
//                is Tcp -> POSLinkSemi.getInstance()
//                    .getTerminal(application, connectionSetting.tcpSetting)
//
//                is Http -> POSLinkSemi.getInstance()
//                    .getTerminal(application, connectionSetting.httpSetting)
//            }
//
//            terminal?.let {
//                val result = it.manage.init()
//                if (result.isSuccessful) {
//                    val appName = result.response().appName()
//                    Log.d(TAG, "Terminal initialized: $appName")
//                    analyticsTracker.logEvent(
//                        "pax_terminal_init_success",
//                        bundleOf("app_name" to appName)
//                    )
//                    onResult(true, appName, it)
//                } else {
//                    val error = result.message()
//                    analyticsTracker.logEvent(
//                        "pax_terminal_init_failure",
//                        bundleOf("error" to error)
//                    )
//                    onResult(false, error, null)
//                }
//            } ?: run {
//                val error =
//                    "Unable to communicate with Terminal. Please make sure terminal is connected to same network as POS"
//                Log.e(TAG, error)
//                analyticsTracker.logEvent("pax_terminal_init_failure", bundleOf("error" to error))
//                onResult(false, error, null)
//            }
//        }
//    }

//    fun startTransaction(
//        terminal: Terminal,
//        amount: String,
//        onSuccess: (DoCreditResponse) -> Unit,
//        onFailure: (String) -> Unit,
//    ) {
//        analyticsTracker.logEvent("pax_transaction_start", bundleOf("amount" to amount))
//
//        runAction {
//            val doCreditRequest = DoCreditRequest().apply {
//                transactionType = TransactionType.SALE
//                amountInformation = AmountRequest().apply {
//                    transactionAmount = formatAmount(amount)
//                }
//                traceInformation = TraceRequest().apply {
//                    ecrReferenceNumber = generateEcrReferenceNumber()
//                }
//                cashierInformation = CashierRequest().apply {
//                    clerkId = getUserDetailsUseCase()?.id.toString()
//                    shiftId = getBatchDetailsUseCase()?.batchNo.toString()
//                }
//            }
//
//            val response = terminal.transaction.doCredit(doCreditRequest)
//
//            if (response.isSuccessful && response.response()?.responseCode() == CODE_SUCCESS) {
//                analyticsTracker.logEvent(
//                    "pax_transaction_success",
//                    bundleOf("pax_transaction_success" to response.response())
//                )
//                onSuccess(response.response())
//            } else {
//                val errorCode = response?.response()?.responseCode()
//                val errorMessage = ("$errorCode /" + (response.response()?.responseMessage()
//                    ?: "Unknown transaction error"))
//                Log.e(TAG, "Transaction failed: $errorMessage")
//                analyticsTracker.logEvent(
//                    "pax_transaction_failure",
//                    bundleOf("error" to "Transaction failed: $errorMessage")
//                )
//                onFailure(errorMessage)
//            }
//        }
//    }

//    fun refundTransaction(
//        terminal: Terminal,
//        amount: String,
//        onSuccess: (DoCreditResponse) -> Unit,
//        onFailure: (String) -> Unit,
//    ) {
//        analyticsTracker.logEvent("pax_refund_start", bundleOf("amount" to amount))
//
//        runAction {
//            val doCreditRequestReturn = DoCreditRequest().apply {
//                transactionType = TransactionType.RETURN
//                amountInformation = AmountRequest().apply {
//                    transactionAmount = formatAmount(amount)
//                }
//                traceInformation = TraceRequest().apply {
//                    ecrReferenceNumber = generateEcrReferenceNumber()
//                }
//                cashierInformation = CashierRequest().apply {
//                    clerkId = getUserDetailsUseCase()?.id.toString()
//                    shiftId = getBatchDetailsUseCase()?.batchNo.toString()
//                }
//            }
//
//            val response = terminal.transaction.doCredit(doCreditRequestReturn)
//
//            if (response.isSuccessful && response.response()?.responseCode() == CODE_SUCCESS) {
//                Log.d(TAG, "refundTransaction: ===========START==============")
//                Log.d(TAG, "Success - " + response.response().responseCode())
//                Log.d(TAG, "Success - " + response.response())
//                analyticsTracker.logEvent(
//                    "pax_refund_success",
//                    bundleOf("pax_refund_success" to response.response())
//                )
//                onSuccess(response.response())
//            } else {
//                Log.e(TAG, "Failure : ${response.message()}")
//                val errorCode = response?.response()?.responseCode()
//                val errorMessage = ("$errorCode /" + (response.response()?.responseMessage()
//                    ?: "Unknown transaction error"))
//                analyticsTracker.logEvent("pax_refund_failure", bundleOf("error" to errorMessage))
//                onFailure(errorMessage)
//                Log.d(TAG, "refundTransaction: ===========END==============")
//            }
//        }
//    }

//    fun voidTransaction(
//        terminal: Terminal,
//        onSuccess: (DoCreditResponse) -> Unit,
//        onFailure: (String) -> Unit,
//        cardDetails: CardDetails
//    ) {
//        analyticsTracker.logEvent(
//            "pax_void_attempt",
//            bundleOf("transaction_no" to cardDetails.transactionNo)
//        )
//
//        runAction {
//            val doVoidCreditTransaction = DoCreditRequest().apply {
//                transactionType = TransactionType.VOID
//                traceInformation = TraceRequest().apply {
//                    originalReferenceNumber = cardDetails.transactionNo
//                    ecrReferenceNumber = cardDetails.ecrReference
//                }
//            }
//
//            val response = terminal.transaction.doCredit(doVoidCreditTransaction)
//
//            if (response.isSuccessful) {
//                response.response()?.let { creditResponse ->
//                    when (creditResponse.responseCode()) {
//                        CODE_SUCCESS, Code100021.ALREADY_VOIDED -> {
//                            analyticsTracker.logEvent(
//                                "pax_void_success",
//                                bundleOf("pax_void_success" to creditResponse)
//                            )
//                            onSuccess(creditResponse)
//                        }
//
//                        else -> {
//                            val errorMessage =
//                                "${creditResponse.responseCode()} / ${creditResponse.responseMessage() ?: "Unknown error"}"
//                            analyticsTracker.logEvent(
//                                "pax_void_failure",
//                                bundleOf("error" to errorMessage)
//                            )
//                            Log.e(TAG, "Transaction Failed: $errorMessage")
//                            onFailure(errorMessage)
//                        }
//                    }
//                } ?: run {
//                    val unknownError = "Transaction Void failed: No response received"
//                    Log.e(TAG, unknownError)
//                    onFailure(unknownError)
//                }
//            } else {
//                val errorMessage = "Transaction failed: ${response.message() ?: "Unknown error"}"
//                Log.e(TAG, errorMessage)
//                analyticsTracker.logEvent("pax_void_failure", bundleOf("error" to errorMessage))
//                onFailure(errorMessage)
//            }
//        }
//    }

//    fun requestPaxBatchClose(
//        terminal: Terminal,
//        onSuccess: (BatchCloseResponse) -> Unit,
//        onFailure: (String) -> Unit
//    ) {
//        analyticsTracker.logEvent("pax_batch_close_attempt", null)
//
//        runAction {
//            val response = terminal.batch.batchClose(BatchCloseRequest())
//            if (response.isSuccessful && response.response()?.responseCode() == CODE_SUCCESS) {
//                analyticsTracker.logEvent(
//                    "pax_batch_close_success",
//                    bundleOf("pax_batch_close_success" to response.response())
//                )
//                onSuccess(response.response())
//            } else {
//                val errorCode = response?.response()?.responseCode()
//                val errorMessage = ("$errorCode / " + (response.response()?.responseMessage()
//                    ?: "Unknown transaction error"))
//                analyticsTracker.logEvent(
//                    "pax_batch_close_failure",
//                    bundleOf("error" to errorMessage)
//                )
//                onFailure(errorMessage)
//            }
//        }
//    }

//    fun getPaxBatchNo(
//        terminal: Terminal,
//        onSuccess: (GetVariableResponse) -> Unit,
//        onFailure: (String) -> Unit
//    ) {
//        analyticsTracker.logEvent("pax_get_batch_no_attempt", null)
//
//        val response = terminal.manage.getVariable(GetVariableRequest().apply {
//            edcType = EdcType.CREDIT
//            variableName1 = "batchNo"
//        })
//
//        if (response.isSuccessful && response.response()?.responseCode() == CODE_SUCCESS) {
//            analyticsTracker.logEvent(
//                "pax_get_batch_no_success",
//                bundleOf("pax_get_batch_no_success" to response.response())
//            )
//            onSuccess(response.response())
//        } else {
//            val errorCode = response?.response()?.responseCode()
//            val errorMessage = ("$errorCode /" + (response.response()?.responseMessage()
//                ?: "Unknown transaction error"))
//            analyticsTracker.logEvent(
//                "pax_get_batch_no_failure",
//                bundleOf("error" to errorMessage)
//            )
//            onFailure(errorMessage)
//        }
//    }

//    fun cancelTransactionBeforeApproved(terminal: Terminal?) {
//        terminal?.cancel()
//        analyticsTracker.logEvent("pax_transaction_cancelled", null)
//    }
//
//    fun verifyTransaction(terminal: Terminal) {
//        analyticsTracker.logEvent("pax_transaction_verification_attempt", null)
//    }

    // --- Utility Methods for Pax Logging ---

    private fun runAction(runnable: Runnable) {
        paxRunningThread?.takeIf { it.isAlive }?.interrupt()
        paxRunningThread = Thread {
            try {
                runnable.run()
            } catch (e: InterruptedException) {
                Log.e(TAG, "Thread interrupted", e)
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                Log.e(TAG, "Thread error : Error in thread execution", e)
            }
        }.also { it.start() }
    }

    private fun formatAmount(amount: String): String {
        return BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal(100)).toInt().toString()
    }

//    private fun getEdcType(edcType: String): EdcType {
//        return EdcType.CREDIT
//    }

    private fun generateEcrReferenceNumber(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8)
    }

    private fun bundleOf(vararg pairs: Pair<String, Any?>): Bundle {
        val bundle = Bundle()
        pairs.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value?.toString())
            }
        }
        return bundle
    }

}

enum class PaxAction {
    RETRY, RESYNC, IGNORE, CONTACT_SUPPORT, REBOOT_TERMINAL, SHOW_DECLINE, USER_CANCEL
}

object PaxResponseMaps {

    private val developerMessages: Map<Int, String> = mapOf(
        0 to "Batch closed successfully (OK)",
        100 to "Declined by acquirer/host",
        100001 to "Batch close timed out – host not responding",
        100002 to "User aborted the operation",
        100004 to "Unsupported transaction type on terminal",
        100005 to "Unsupported EDC operation",
        100006 to "Batch settlement failed – possible corrupt transaction",
        100007 to "Connect error – terminal could not reach gateway",
        100008 to "Send error – device failed to deliver request",
        100009 to "Receive error – no response from host",
        100010 to "Communication error between POS and terminal",
        100011 to "Duplicate transaction detected (local)",
        100021 to "Already voided / completed transaction",
        100023 to "Not found – referenced batch/transaction not present",
        100511 to "Host/service specific error (see processor docs)"
    )

    private val userMessages: Map<Int, String> = mapOf(
        0 to "Batch closed successfully.",
        100 to "Payment declined. Try a different card.",
        100001 to "Batch closing timed out. Please try again.",
        100002 to "Operation cancelled.",
        100004 to "This operation is not supported on this terminal.",
        100005 to "This operation is not supported.",
        100006 to "Batch settlement failed. Please retry later or contact support.",
        100007 to "Unable to connect to payment server. Check network.",
        100008 to "Failed to send payment request. Try again.",
        100009 to "No response from the payment server. Try again.",
        100010 to "Communication error with terminal. Check connection.",
        100011 to "This transaction was already processed.",
        100021 to "Transaction already completed or voided.",
        100023 to "Referenced batch/transaction not found.",
        100511 to "Payment service error. Contact support."
    )

    private val recommendedActions: Map<Int, PaxAction> = mapOf(
        0 to PaxAction.IGNORE,
        100 to PaxAction.SHOW_DECLINE,
        100001 to PaxAction.RETRY,
        100002 to PaxAction.IGNORE,
        100004 to PaxAction.IGNORE,
        100005 to PaxAction.IGNORE,
        100006 to PaxAction.RESYNC,
        100007 to PaxAction.RETRY,
        100008 to PaxAction.RETRY,
        100009 to PaxAction.RETRY,
        100010 to PaxAction.REBOOT_TERMINAL,
        100011 to PaxAction.IGNORE,
        100021 to PaxAction.IGNORE,
        100023 to PaxAction.RESYNC,
        100511 to PaxAction.CONTACT_SUPPORT
    )

    fun developerMessage(code: Int): String = developerMessages[code] ?: "Unknown PAX error (code: $code)"
    fun userMessage(code: Int): String = userMessages[code] ?: "Something went wrong (code: $code)"
    fun recommendedAction(code: Int): PaxAction = recommendedActions[code] ?: PaxAction.CONTACT_SUPPORT
}

