package com.retail.dolphinpos.domain.usecases.label

import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo
import com.retail.dolphinpos.domain.repositories.label.LabelPrinterRepository
import javax.inject.Inject

class GetAvailableLabelPrintersUseCase @Inject constructor(
    private val labelPrinterRepository: LabelPrinterRepository
) {
    suspend operator fun invoke(): List<DiscoveredPrinterInfo> {
        return labelPrinterRepository.getAvailablePrinters()
    }
}

