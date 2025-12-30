package com.retail.dolphinpos.presentation.features.ui.setup.label_printer

data class LabelPrintingVariantModel(
    val productId: Int,
    val productName: String,
    val variantId: Int?,
    val variantName: String?,
    val barcode: String,
    val quantity: Int = 1
)

data class DiscoveredPrinterInfo(
    val modelName: String,
    val address: String,
    val connectionType: String
)

