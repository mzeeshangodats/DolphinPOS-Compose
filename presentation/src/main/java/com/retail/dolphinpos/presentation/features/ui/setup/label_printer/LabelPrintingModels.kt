package com.retail.dolphinpos.presentation.features.ui.setup.label_printer

import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo

data class LabelPrintingVariantModel(
    val productId: Int,
    val productName: String,
    val variantId: Int?,
    val variantName: String?,
    val barcode: String,
    val quantity: Int = 1,
    val cashPrice: Double = 0.0,
    val cardPrice: Double = 0.0,
    val cashDiscountedPrice: Double = 0.0,
    val cardDiscountedPrice: Double = 0.0,
    val isDiscounted: Boolean = false,
    val applyDualPrice: Boolean = true
)

