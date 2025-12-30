package com.retail.dolphinpos.presentation.features.ui.setup.label_printer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import com.retail.dolphinpos.domain.usecases.label.CancelPrintJobUseCase
import com.retail.dolphinpos.domain.usecases.label.GetAvailableLabelPrintersUseCase
import com.retail.dolphinpos.domain.usecases.label.GetDualPricePercentageUseCase
import com.retail.dolphinpos.domain.usecases.label.PrintLabelUseCase
import com.retail.dolphinpos.presentation.features.ui.setup.label_printer.toLabels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val homeRepository: HomeRepository,
    private val getAvailableLabelPrintersUseCase: GetAvailableLabelPrintersUseCase,
    private val printLabelUseCase: PrintLabelUseCase,
    private val getDualPricePercentageUseCase: GetDualPricePercentageUseCase,
    private val cancelPrintJobUseCase: CancelPrintJobUseCase
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
            delay(50)
            try {
                emitViewEffect(LabelPrintingViewEffect.Loading(true))
                val products = homeRepository.getAllProducts()
                _viewState.value = _viewState.value.copy(products = products)
                emitViewEffect(LabelPrintingViewEffect.Loading(false))
            } catch (e: Exception) {
                emitViewEffect(LabelPrintingViewEffect.Loading(false))
                emitViewEffect(LabelPrintingViewEffect.ShowErrorSnackBar("Failed to load products: ${e.message}"))
            }
        }
    }

    fun startSearchUSBPrinter(context: Context?) {
        viewModelScope.launch {
            if (context == null) return@launch
            emitViewEffect(LabelPrintingViewEffect.Loading(true))
            try {
                val printers = getAvailableLabelPrintersUseCase()
                emitViewEffect(LabelPrintingViewEffect.Loading(false))
                
                if (printers.isEmpty()) {
                    emitViewEffect(LabelPrintingViewEffect.ShowErrorSnackBar("No Printers Discovered"))
                } else {
                    emitViewEffect(LabelPrintingViewEffect.ShowPrintDialog(printers))
                }
            } catch (e: Exception) {
                emitViewEffect(LabelPrintingViewEffect.Loading(false))
                val errorMessage = when {
                    e.message?.contains("permission", ignoreCase = true) == true -> {
                        "USB permission not granted. Please grant USB permission for the printer."
                    }
                    else -> {
                        "Failed to search printers: ${e.message ?: "Unknown error"}"
                    }
                }
                emitViewEffect(LabelPrintingViewEffect.ShowErrorSnackBar(errorMessage))
            }
        }
    }

    fun mapToLabelPrintingVariants(product: com.retail.dolphinpos.domain.model.home.catrgories_products.Products): List<LabelPrintingVariantModel> {
        val labelVariants = mutableListOf<LabelPrintingVariantModel>()
        val dualPricePercentage = getDualPricePercentageUseCase()

        val price = product.price?.toDoubleOrNull() ?: 0.0
        val discountedPrice = 0.0 // TODO: Get from product if discount field exists

        if (product.variants?.isEmpty() == true) {
            labelVariants.add(
                LabelPrintingVariantModel(
                    productId = product.id,
                    productName = product.name ?: "",
                    variantId = null,
                    variantName = null,
                    quantity = 1,
                    barcode = product.barCode ?: "",
                    cashPrice = price,
                    cardPrice = (price + (price * dualPricePercentage)).formatToTwoDecimals(),
                    cardDiscountedPrice = (discountedPrice + (discountedPrice * dualPricePercentage)).formatToTwoDecimals(),
                    isDiscounted = false, // TODO: Get from product if available
                    //discountedPrice = discountedPrice,
                    applyDualPrice = true
                )
            )
        }

        product.variants?.map { variant ->
            val variantPrice = variant.price?.toDoubleOrNull() ?: 0.0
            val variantDiscountedPrice = 0.0 // TODO: Get from variant if discount field exists

            labelVariants.add(
                LabelPrintingVariantModel(
                    productId = product.id,
                    productName = product.name ?: "",
                    variantId = variant.id,
                    variantName = variant.title,
                    quantity = 1,
                    barcode = variant.barCode ?: product.barCode ?: "",
                    cashPrice = variantPrice,
                    cardPrice = (variantPrice + (variantPrice * dualPricePercentage)).formatToTwoDecimals(),
                    isDiscounted = false, // TODO: Get from variant if available
                    //discountedPrice = variantDiscountedPrice,
                    cardDiscountedPrice = (variantDiscountedPrice + (variantDiscountedPrice * dualPricePercentage)).formatToTwoDecimals(),
                    applyDualPrice = true
                )
            )
        }

        return labelVariants
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
        viewModelScope.launch {
            if (_viewState.value.selectedVariants.isEmpty()) {
                emitViewEffect(LabelPrintingViewEffect.ShowErrorSnackBar("Please add variants to print"))
                return@launch
            }

            // First check and request USB permission if needed
            emitViewEffect(LabelPrintingViewEffect.CheckAndSearchPrinters)
        }
    }

    fun onStartPrintingClicked(printer: DiscoveredPrinterInfo) {
        viewModelScope.launch {
            try {
                val result = printLabelUseCase(printer, _viewState.value.selectedVariants.toLabels())
                if (result.isSuccess) {
                    emitViewEffect(LabelPrintingViewEffect.ShowSuccessSnackBar("Printed Successfully"))
                } else {
                    emitViewEffect(LabelPrintingViewEffect.ShowDialog("${result.exceptionOrNull()?.message}"))
                }
            } catch (e: Exception) {
                emitViewEffect(LabelPrintingViewEffect.ShowDialog("Error: ${e.message}"))
            }
        }
    }

    fun onCancelPrintClicked() {
        viewModelScope.launch {
            cancelPrintJobUseCase()
        }
    }

    private fun emitViewEffect(effect: LabelPrintingViewEffect) {
        viewModelScope.launch {
            _viewEffect.emit(effect)
        }
    }
}

private fun Double.formatToTwoDecimals(): Double {
    return String.format("%.2f", this).toDoubleOrNull() ?: this
}

