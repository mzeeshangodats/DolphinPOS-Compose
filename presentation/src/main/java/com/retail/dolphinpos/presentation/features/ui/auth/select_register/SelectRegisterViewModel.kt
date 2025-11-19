package com.retail.dolphinpos.presentation.features.ui.auth.select_register

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.auth.select_registers.request.UpdateStoreRegisterRequest
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectRegisterViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val storeRegistersRepository: StoreRegistersRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _selectRegisterUiEvent = MutableSharedFlow<SelectRegisterUiEvent>()
    val selectRegisterUiEvent = _selectRegisterUiEvent.asSharedFlow()

    init {
        getStoreLocations()
    }

    fun getStoreLocations() {
        viewModelScope.launch {
            _selectRegisterUiEvent.emit(SelectRegisterUiEvent.ShowLoading)
            try {
                val response = storeRegistersRepository.getLocations(preferenceManager.getStoreID())
                _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                _selectRegisterUiEvent.emit(
                    SelectRegisterUiEvent.PopulateLocationsList(response)
                )
                
                if (response.isEmpty()) {
                    _selectRegisterUiEvent.emit(
                        SelectRegisterUiEvent.PopulateRegistersList(emptyList())
                    )
                } else {
                    getStoreRegisters(response[0].id)
                }

            } catch (e: Exception) {
                _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                _selectRegisterUiEvent.emit(
                    SelectRegisterUiEvent.ShowError(e.message ?: "Something went wrong")
                )
            }
        }
    }

    fun getStoreRegisters(locationID: Int) {
        viewModelScope.launch {
            _selectRegisterUiEvent.emit(SelectRegisterUiEvent.ShowLoading)
            try {
                val response =
                    storeRegistersRepository.getRegistersByLocationID(locationID)
                _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                _selectRegisterUiEvent.emit(
                    SelectRegisterUiEvent.PopulateRegistersList(response)
                )

            } catch (e: Exception) {
                _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                _selectRegisterUiEvent.emit(
                    SelectRegisterUiEvent.ShowError(e.message ?: "Something went wrong")
                )
            }
        }
    }

    fun updateStoreRegister(locationID: Int, storeRegisterID: Int) {
        viewModelScope.launch {
            _selectRegisterUiEvent.emit(SelectRegisterUiEvent.ShowLoading)
            try {
                val response = storeRegistersRepository.updateStoreRegister(
                    UpdateStoreRegisterRequest(
                        preferenceManager.getStoreID(),
                        locationID,
                        storeRegisterID
                    )
                )
                response.message?.let {
                    preferenceManager.setRegister(true)
                    preferenceManager.setOccupiedLocationID(locationID)
                    preferenceManager.setOccupiedRegisterID(storeRegisterID)
                    storeRegistersRepository.insertRegisterStatusDetailsIntoLocalDB(response.data)
                    _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                    _selectRegisterUiEvent.emit(SelectRegisterUiEvent.NavigateToPinScreen)
                } ?: run {
                    _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                }

            } catch (e: Exception) {
                _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                _selectRegisterUiEvent.emit(
                    SelectRegisterUiEvent.ShowError(e.message ?: "Something went wrong")
                )
            }
        }
    }

    fun getProducts(locationID: Int, storeRegisterID: Int, selectedRegister: com.retail.dolphinpos.domain.model.auth.login.response.Registers) {
        viewModelScope.launch {
            _selectRegisterUiEvent.emit(SelectRegisterUiEvent.ShowLoading)
            try {
                // Save the selected register to database
                storeRegistersRepository.insertRegisterIntoLocalDB(selectedRegister)
                
                val response =
                    storeRegistersRepository.getProducts(preferenceManager.getStoreID(), locationID)
                if (response.category?.categories.isNullOrEmpty()) {
                    _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                    _selectRegisterUiEvent.emit(
                        SelectRegisterUiEvent.ShowError("No categories found in response")
                    )
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
                _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                updateStoreRegister(locationID, storeRegisterID)

            } catch (e: Exception) {
                _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                _selectRegisterUiEvent.emit(
                    SelectRegisterUiEvent.ShowError(e.message ?: "Something went wrong")
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _selectRegisterUiEvent.emit(SelectRegisterUiEvent.ShowLoading)
            try {
                val response = storeRegistersRepository.logout()
                response.message.let {
                    preferenceManager.setLogin(false)
                    _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                    _selectRegisterUiEvent.emit(SelectRegisterUiEvent.NavigateToLoginScreen)
                }

            } catch (e: Exception) {
                _selectRegisterUiEvent.emit(SelectRegisterUiEvent.HideLoading)
                _selectRegisterUiEvent.emit(
                    SelectRegisterUiEvent.ShowError(e.message ?: "Something went wrong")
                )
            }
        }
    }

    fun getName(): String {
        return preferenceManager.getName()
    }
}