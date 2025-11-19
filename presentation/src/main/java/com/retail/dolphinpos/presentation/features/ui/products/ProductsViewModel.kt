package com.retail.dolphinpos.presentation.features.ui.products

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
class ProductsViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val storeRegistersRepository: StoreRegistersRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _products = MutableStateFlow<List<Products>>(emptyList())
    val products: StateFlow<List<Products>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ProductsUiEvent>()
    val uiEvent: SharedFlow<ProductsUiEvent> = _uiEvent.asSharedFlow()

    fun loadAllProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val productsList = homeRepository.getAllProducts()
                _products.value = productsList
            } catch (e: Exception) {
                _uiEvent.emit(ProductsUiEvent.ShowError("Failed to load products: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncProducts() {
        viewModelScope.launch {
            _uiEvent.emit(ProductsUiEvent.ShowLoading)
            try {
                val storeID = preferenceManager.getStoreID()
                val locationID = preferenceManager.getOccupiedLocationID()
                
                if (storeID == 0 || locationID == 0) {
                    _uiEvent.emit(ProductsUiEvent.HideLoading)
                    _uiEvent.emit(ProductsUiEvent.ShowError("Store ID or Location ID not found"))
                    return@launch
                }
                
                val response = storeRegistersRepository.getProducts(storeID, locationID)
                
                if (response.category?.categories.isNullOrEmpty()) {
                    _uiEvent.emit(ProductsUiEvent.HideLoading)
                    _uiEvent.emit(ProductsUiEvent.ShowError("No categories found in response"))
                    return@launch
                }
                
                // Delete all existing products data before syncing new data
                storeRegistersRepository.deleteAllProductsData()
                
                // Collect all image URLs for downloading
                val allImageUrls = mutableListOf<String>()
                
                response.category!!.categories.forEach { category ->
                    // Insert category
                    storeRegistersRepository.insertCategoriesIntoLocalDB(listOf(category))
                    category.products!!.forEach { product ->
                        // Insert products
                        storeRegistersRepository.insertProductsIntoLocalDB(
                            listOf(product),
                            category.id
                        )
                        // Insert product images
                        storeRegistersRepository.insertProductImagesIntoLocalDB(
                            product.images,
                            product.id
                        )
                        
                        // Collect product image URLs
                        product.images?.forEach { image ->
                            image.fileURL?.let { allImageUrls.add(it) }
                        }
                        
                        // Insert vendor
                        product.vendor?.let {
                            storeRegistersRepository.insertVendorDetailsIntoLocalDB(it, product.id)
                        }
                        // Insert variants
                        product.variants!!.forEach { variant ->
                            storeRegistersRepository.insertProductVariantsIntoLocalDB(
                                listOf(variant),
                                product.id
                            )
                            // Insert variant images
                            storeRegistersRepository.insertVariantImagesIntoLocalDB(
                                variant.images,
                                variant.id
                            )
                            
                            // Collect variant image URLs
                            variant.images.forEach { image ->
                                image.fileURL?.let { allImageUrls.add(it) }
                            }
                        }
                    }
                }
                
                // Download and cache all images
                if (allImageUrls.isNotEmpty()) {
                    try {
                        storeRegistersRepository.downloadAndCacheImages(allImageUrls)
                    } catch (e: Exception) {
                        // Log error but don't fail the entire operation
                        e.printStackTrace()
                    }
                }
                
                // Clear old cached images to manage storage
                try {
                    storeRegistersRepository.clearOldCachedImages()
                } catch (e: Exception) {
                    // Log error but don't fail the entire operation
                    e.printStackTrace()
                }
                
                _uiEvent.emit(ProductsUiEvent.HideLoading)
                
                // Reload products after sync
                loadAllProducts()
                
            } catch (e: Exception) {
                _uiEvent.emit(ProductsUiEvent.HideLoading)
                _uiEvent.emit(ProductsUiEvent.ShowError(e.message ?: "Failed to sync products"))
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            // Just clear preferences and navigate, no API call
            _uiEvent.emit(ProductsUiEvent.NavigateToPinCode)
        }
    }
}

sealed class ProductsUiEvent {
    object ShowLoading : ProductsUiEvent()
    object HideLoading : ProductsUiEvent()
    object NavigateToPinCode : ProductsUiEvent()
    data class ShowError(val message: String) : ProductsUiEvent()
}
