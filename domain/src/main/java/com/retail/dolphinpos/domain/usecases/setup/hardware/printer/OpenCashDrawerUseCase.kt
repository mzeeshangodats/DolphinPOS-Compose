package com.retail.dolphinpos.domain.usecases.setup.hardware.printer

interface OpenCashDrawerUseCase {
    suspend operator fun invoke(
        onStatusUpdate: (String) -> Unit
    ): Pair<Boolean, String>
}

