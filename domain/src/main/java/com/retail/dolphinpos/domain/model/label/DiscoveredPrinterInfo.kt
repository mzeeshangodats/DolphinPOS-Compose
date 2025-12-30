package com.retail.dolphinpos.domain.model.label

data class DiscoveredPrinterInfo(
    val modelName: String,
    val address: String,
    val connectionType: String,
    val extraInfo: Map<String, String>? = null
)

