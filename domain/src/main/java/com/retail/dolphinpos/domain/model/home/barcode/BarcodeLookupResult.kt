package com.retail.dolphinpos.domain.model.home.barcode

import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant

sealed class BarcodeLookupResult {
    data class SingleProduct(val product: Products) : BarcodeLookupResult()
    data class SingleVariant(val product: Products, val variant: Variant) : BarcodeLookupResult()
    data class MultipleVariants(val product: Products, val variants: List<Variant>) : BarcodeLookupResult()
    object NotFound : BarcodeLookupResult()
}

