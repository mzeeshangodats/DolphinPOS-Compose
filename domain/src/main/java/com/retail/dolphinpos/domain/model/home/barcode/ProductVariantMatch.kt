package com.retail.dolphinpos.domain.model.home.barcode

import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant

data class ProductVariantMatch(
    val product: Products,
    val variant: Variant
)

