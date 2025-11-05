package com.retail.dolphinpos.presentation.features.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val storeRegistersRepository: StoreRegistersRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _products = MutableStateFlow<List<Products>>(emptyList())
    val products: StateFlow<List<Products>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<InventoryUiEvent>()
    val uiEvent: SharedFlow<InventoryUiEvent> = _uiEvent.asSharedFlow()

    fun loadAllProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val productsList = homeRepository.getAllProducts()
                _products.value = productsList
            } catch (e: Exception) {
                _uiEvent.emit(InventoryUiEvent.ShowError("Failed to load products: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Just clear preferences and navigate, no API call
            _uiEvent.emit(InventoryUiEvent.NavigateToPinCode)
        }
    }
}

sealed class InventoryUiEvent {
    object ShowLoading : InventoryUiEvent()
    object HideLoading : InventoryUiEvent()
    object NavigateToPinCode : InventoryUiEvent()
    data class ShowError(val message: String) : InventoryUiEvent()
}
