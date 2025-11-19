package com.retail.dolphinpos.presentation.features.ui.setup.business_info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.domain.usecases.auth.GetStoreDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BusinessInformationUiEvent {
    data class ShowSuccess(val message: String) : BusinessInformationUiEvent()
    data class ShowError(val message: String) : BusinessInformationUiEvent()
}

@HiltViewModel
class BusinessInformationViewModel @Inject constructor(
    private val getStoreDetailsUseCase: GetStoreDetailsUseCase,
    private val userDao: UserDao,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _storeName = MutableStateFlow("")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _addressLine1 = MutableStateFlow("")
    val addressLine1: StateFlow<String> = _addressLine1.asStateFlow()

    private val _addressLine2 = MutableStateFlow("")
    val addressLine2: StateFlow<String> = _addressLine2.asStateFlow()

    private val _zipCode = MutableStateFlow("")
    val zipCode: StateFlow<String> = _zipCode.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<BusinessInformationUiEvent>()
    val uiEvent: SharedFlow<BusinessInformationUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadStoreInformation()
    }

    fun loadStoreInformation() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get store information
                val store = getStoreDetailsUseCase()
                store?.let {
                    _storeName.value = it.name ?: ""
                    // Split address by newline if it exists, otherwise use full address
                    val address = it.address ?: ""
                    val addressLines = address.split("\n", limit = 2)
                    _addressLine1.value = addressLines.getOrNull(0) ?: ""
                    _addressLine2.value = addressLines.getOrNull(1) ?: ""
                }

                // Get location information for zip code
                val locationId = preferenceManager.getOccupiedLocationID()
                if (locationId > 0) {
                    try {
                        val location = userDao.getLocationByLocationId(locationId)
                        _zipCode.value = location.zipCode ?: ""
                    } catch (e: Exception) {
                        // Location not found, leave zip code empty
                    }
                }
            } catch (e: Exception) {
                // Handle error - leave fields empty
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStoreName(value: String) {
        _storeName.value = value
    }

    fun updateAddressLine1(value: String) {
        _addressLine1.value = value
    }

    fun updateAddressLine2(value: String) {
        _addressLine2.value = value
    }

    fun updateZipCode(value: String) {
        _zipCode.value = value
    }

    fun updatePhoneNumber(value: String) {
        _phoneNumber.value = value
    }

    fun saveStoreInformation() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current store entity
                val currentStore = userDao.getStore()
                
                // Combine address lines
                val fullAddress = if (_addressLine2.value.isNotEmpty()) {
                    "${_addressLine1.value}\n${_addressLine2.value}"
                } else {
                    _addressLine1.value
                }

                // Update store entity
                val updatedStore = currentStore.copy(
                    name = _storeName.value.ifEmpty { null },
                    address = fullAddress.ifEmpty { null }
                )

                // Save to database (using REPLACE strategy)
                userDao.insertStoreDetails(updatedStore)

                // Update location zip code if location exists
                val locationId = preferenceManager.getOccupiedLocationID()
                if (locationId > 0) {
                    try {
                        val currentLocation = userDao.getLocationByLocationId(locationId)
                        val updatedLocation = currentLocation.copy(
                            zipCode = _zipCode.value.ifEmpty { null }
                        )
                        userDao.insertLocations(updatedLocation)
                    } catch (e: Exception) {
                        // Location not found, skip update
                    }
                }

                // Note: Phone number is not stored in StoreEntity or LocationEntity
                // If needed, this would require extending the entities
                
                _uiEvent.emit(BusinessInformationUiEvent.ShowSuccess("Business information saved successfully"))
            } catch (e: Exception) {
                _uiEvent.emit(BusinessInformationUiEvent.ShowError("Failed to save business information: ${e.message ?: "Unknown error"}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}

