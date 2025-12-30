package com.retail.dolphinpos.data.repositories.label

import android.content.Context
import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo
import com.retail.dolphinpos.domain.usecases.label.PrinterSearchError

interface IPrinterSearcher {
    fun start(
        context: Context,
        targetModels: Array<String>,
        callback: (PrinterSearchError?, com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode?, ArrayList<DiscoveredPrinterInfo>) -> Unit
    )

    fun cancel()
}
