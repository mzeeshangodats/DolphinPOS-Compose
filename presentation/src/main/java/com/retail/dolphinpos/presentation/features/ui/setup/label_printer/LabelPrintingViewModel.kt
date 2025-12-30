package com.retail.dolphinpos.presentation.features.ui.setup.label_printer

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LabelPrintingViewEffect {
    object NavigateToBack : LabelPrintingViewEffect()
    data class ShowErrorSnackBar(val message: String) : LabelPrintingViewEffect()
    data class ShowInformationSnackBar(val message: String) : LabelPrintingViewEffect()
    data class ShowSuccessSnackBar(val message: String) : LabelPrintingViewEffect()
    data class Loading(val isLoading: Boolean) : LabelPrintingViewEffect()
    data class ShowPrintDialog(val printers: List<DiscoveredPrinterInfo>) : LabelPrintingViewEffect()
    object CheckAndSearchPrinters : LabelPrintingViewEffect()
    data class ShowDialog(val message: String) : LabelPrintingViewEffect()
}

data class LabelPrintingViewState(
    val products: List<com.retail.dolphinpos.domain.model.home.catrgories_products.Products> = emptyList(),
    val selectedVariants: List<LabelPrintingVariantModel> = emptyList(),
    val isPrintButtonEnabled: Boolean = false
)

@HiltViewModel
class LabelPrintingViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(LabelPrintingViewState())
    val viewState: StateFlow<LabelPrintingViewState> = _viewState

    private val _viewEffect = MutableSharedFlow<LabelPrintingViewEffect>()
    val viewEffect: SharedFlow<LabelPrintingViewEffect> = _viewEffect.asSharedFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                val products = homeRepository.getAllProducts()
                _viewState.value = _viewState.value.copy(products = products)
            } catch (e: Exception) {
                emitViewEffect(LabelPrintingViewEffect.ShowErrorSnackBar("Failed to load products: ${e.message}"))
            }
        }
    }

    fun mapToLabelPrintingVariants(product: com.retail.dolphinpos.domain.model.home.catrgories_products.Products): List<LabelPrintingVariantModel> {
        val variants = product.variants ?: emptyList()
        return if (variants.isEmpty()) {
            // If no variants, create a single variant from the product itself
            listOf(
                LabelPrintingVariantModel(
                    productId = product.id,
                    productName = product.name ?: "",
                    variantId = null,
                    variantName = null,
                    barcode = product.barCode ?: "",
                    quantity = 1
                )
            )
        } else {
            variants.map { variant ->
                LabelPrintingVariantModel(
                    productId = product.id,
                    productName = product.name ?: "",
                    variantId = variant.id,
                    variantName = variant.title,
                    barcode = variant.barCode ?: product.barCode ?: "",
                    quantity = 1
                )
            }
        }
    }

    fun onUpdateVariants(variants: List<LabelPrintingVariantModel>) {
        _viewState.value = _viewState.value.copy(
            selectedVariants = variants,
            isPrintButtonEnabled = variants.isNotEmpty()
        )
    }

    fun onBackClicked() {
        emitViewEffect(LabelPrintingViewEffect.NavigateToBack)
    }

    fun onPrintClicked() {
        if (_viewState.value.selectedVariants.isEmpty()) {
            emitViewEffect(LabelPrintingViewEffect.ShowErrorSnackBar("Please add variants to print"))
            return
        }
        // Check for USB printers
        emitViewEffect(LabelPrintingViewEffect.CheckAndSearchPrinters)
    }

    fun startSearchUSBPrinter(context: Context?) {
        viewModelScope.launch {
            if (context == null) return@launch
            emitViewEffect(LabelPrintingViewEffect.Loading(true))
            try {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val brotherDevices = usbManager.deviceList.values.filter { it.vendorId == 0x04F9 }
                
                val printers = brotherDevices.map { device ->
                    DiscoveredPrinterInfo(
                        modelName = "${device.manufacturerName ?: "Brother"} ${device.productName ?: "Printer"}",
                        address = device.deviceName ?: "",
                        connectionType = "USB"
                    )
                }
                
                emitViewEffect(LabelPrintingViewEffect.Loading(false))
                emitViewEffect(LabelPrintingViewEffect.ShowPrintDialog(printers))
            } catch (e: Exception) {
                emitViewEffect(LabelPrintingViewEffect.Loading(false))
                emitViewEffect(LabelPrintingViewEffect.ShowErrorSnackBar("Failed to search printers: ${e.message}"))
            }
        }
    }

    fun onStartPrintingClicked(printer: DiscoveredPrinterInfo) {
        viewModelScope.launch {
            emitViewEffect(LabelPrintingViewEffect.Loading(true))
            try {
                // TODO: Implement actual printing logic
                // For now, just show success
                emitViewEffect(LabelPrintingViewEffect.Loading(false))
                emitViewEffect(LabelPrintingViewEffect.ShowSuccessSnackBar("Printing started successfully"))
            } catch (e: Exception) {
                emitViewEffect(LabelPrintingViewEffect.Loading(false))
                emitViewEffect(LabelPrintingViewEffect.ShowErrorSnackBar("Failed to print: ${e.message}"))
            }
        }
    }

    private fun emitViewEffect(effect: LabelPrintingViewEffect) {
        viewModelScope.launch {
            _viewEffect.emit(effect)
        }
    }
}

