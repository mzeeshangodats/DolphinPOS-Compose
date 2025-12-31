package com.retail.dolphinpos.domain.usecases.label

import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo
import com.retail.dolphinpos.domain.model.label.Label
import com.retail.dolphinpos.domain.repositories.label.LabelPrinterRepository
import javax.inject.Inject

class PrintLabelUseCase @Inject constructor(
    private val labelPrinterRepository: LabelPrinterRepository
) {
    suspend operator fun invoke(
        printerDevice: DiscoveredPrinterInfo,
        labels: List<Label>
    ): Result<Unit> {
        return labelPrinterRepository.connectAndPrintLabels(printerDevice, labels)
    }
}

