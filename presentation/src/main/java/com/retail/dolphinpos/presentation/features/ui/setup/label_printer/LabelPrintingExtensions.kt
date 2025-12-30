package com.retail.dolphinpos.presentation.features.ui.setup.label_printer

import com.retail.dolphinpos.domain.model.label.Label

fun List<LabelPrintingVariantModel>.toLabels(): List<Label> {
    return this.map { variant ->
        Label(
            productName = variant.productName,
            variantName = variant.variantName ?: "",
            barcode = variant.barcode,
            cashPrice = variant.cashPrice,
            cardPrice = variant.cardPrice,
            cashDiscountedPrice = variant.cashDiscountedPrice,
            cardDiscountedPrice = variant.cardDiscountedPrice,
            isDiscounted = variant.isDiscounted,
            applyDualPrice = variant.applyDualPrice
        )
    }
}

