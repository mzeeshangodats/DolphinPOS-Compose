package com.retail.dolphinpos.data.usecases.home

import com.retail.dolphinpos.domain.model.home.barcode.BarcodeLookupResult
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import com.retail.dolphinpos.domain.usecases.home.GetProductByBarcodeUseCase
import javax.inject.Inject

class GetProductByBarcodeUseCaseImpl @Inject constructor(
    private val homeRepository: HomeRepository
) : GetProductByBarcodeUseCase {

    override suspend fun invoke(barcode: String): BarcodeLookupResult {
        val sanitizedBarcode = barcode.trim()
        if (sanitizedBarcode.isEmpty()) {
            return BarcodeLookupResult.NotFound
        }

        homeRepository.getVariantByBarcode(sanitizedBarcode)?.let { match ->
            return BarcodeLookupResult.SingleVariant(match.product, match.variant)
        }

        val product = homeRepository.getProductByBarcode(sanitizedBarcode)
        if (product != null) {
            val variants = product.variants.orEmpty()
            return if (variants.isEmpty()) {
                BarcodeLookupResult.SingleProduct(product)
            } else {
                BarcodeLookupResult.MultipleVariants(product, variants)
            }
        }

        return BarcodeLookupResult.NotFound
    }
}

