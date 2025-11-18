package com.retail.dolphinpos.domain.usecases.setup.hardware.printer

interface TestPrintUseCase {
    suspend operator fun invoke(
        isGraphicPrinter: Boolean = false,
        statusCallback: (String) -> Unit = {}
    ): String
}

