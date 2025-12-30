package com.retail.dolphinpos.domain.usecases.label

import com.retail.dolphinpos.domain.repositories.label.LabelPrinterRepository
import javax.inject.Inject

class CancelPrintJobUseCase @Inject constructor(
    private val labelPrinterRepository: LabelPrinterRepository
) {
    suspend operator fun invoke() {
        labelPrinterRepository.cancelPrintJob()
    }
}

