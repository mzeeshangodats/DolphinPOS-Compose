package com.retail.dolphinpos.domain.usecases.home

import com.retail.dolphinpos.domain.model.home.barcode.BarcodeLookupResult

interface GetProductByBarcodeUseCase {
    suspend operator fun invoke(barcode: String): BarcodeLookupResult
}

