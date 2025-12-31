package com.retail.dolphinpos.domain.repositories.label

import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo
import com.retail.dolphinpos.domain.model.label.Label

interface LabelPrinterRepository {
    suspend fun getAvailablePrinters(): List<DiscoveredPrinterInfo>
    suspend fun connectAndPrintLabels(printerDevice: DiscoveredPrinterInfo, labels: List<Label>): Result<Unit>
    suspend fun cancelPrintJob(): Result<Unit>
}

