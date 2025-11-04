package com.retail.dolphinpos.data.setup.hardware.payment.pax

import com.pax.poslinksemiintegration.transaction.DoCreditResponse
import com.retail.dolphinpos.domain.model.home.create_order.CardDetails
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.ProcessTransactionResult
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.ProcessTransactionUseCase
import javax.inject.Inject

class ProcessTransactionUseCaseImpl @Inject constructor(
    private val paxManager: PaxManager,
    private val sessionManager: TerminalSessionManager
) : ProcessTransactionUseCase {

    override suspend operator fun invoke(
        sessionId: String,
        amount: String,
        onResult: (ProcessTransactionResult) -> Unit
    ) {
        val terminal = sessionManager.getTerminal(sessionId)
        if (terminal == null) {
            onResult(
                ProcessTransactionResult(
                    isSuccess = false,
                    message = "Terminal session not found or expired",
                    cardDetails = null
                )
            )
            return
        }

        paxManager.startTransaction(
            terminal = terminal,
            amount = amount,
            onSuccess = { response: DoCreditResponse ->
                val cardDetails = mapResponseToCardDetails(response)
                onResult(
                    ProcessTransactionResult(
                        isSuccess = true,
                        message = "Transaction successful",
                        cardDetails = cardDetails
                    )
                )
            },
            onFailure = { errorMessage: String ->
                onResult(
                    ProcessTransactionResult(
                        isSuccess = false,
                        message = errorMessage,
                        cardDetails = null
                    )
                )
            }
        )
    }

    private fun mapResponseToCardDetails(response: DoCreditResponse): CardDetails {
        return CardDetails(
            terminalInvoiceNo = response.invoiceNumber() ?: "",
            transactionId = response.hostInformation()?.transactionNumber() ?: "",
            authCode = response.hostInformation()?.authorizationCode() ?: "",
            rrn = response.hostInformation()?.retrievalReferenceNumber() ?: "",
            brand = response.cardInformation()?.cardBrand() ?: "",
            last4 = response.cardInformation()?.accountNumber()?.takeLast(4) ?: "",
            entryMethod = response.cardInformation()?.entryMode() ?: "",
            merchantId = /*response.merchantInformation()?.merchantId() ?:*/ "",
            terminalId = response.merchantInformation()?.terminalId() ?: ""
        )
    }
}
