package com.retail.dolphinpos.domain.usecases.label

sealed class PrinterSearchError {
    object None : PrinterSearchError()
    object USBPermissionNotGrant : PrinterSearchError()
}

