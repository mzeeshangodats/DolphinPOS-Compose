package com.retail.dolphinpos.presentation.features.ui.auth.pin_code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import com.retail.dolphinpos.domain.usecases.GetCurrentTimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerifyPinViewModel @Inject constructor(
    private val repository: VerifyPinRepository,
    private val preferenceManager: PreferenceManager,
    private val getCurrentTimeUseCase: GetCurrentTimeUseCase
) : ViewModel() {
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate

    private val _verifyPinUiEvent = MutableSharedFlow<VerifyPinUiEvent>()
    val verifyPinUiEvent = _verifyPinUiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            getCurrentTimeUseCase().collect { (time, date) ->
                _currentDate.value = date
                _currentTime.value = time
            }
        }
    }

    fun verifyPin(
        pin: String
    ) {
        viewModelScope.launch {
            _verifyPinUiEvent.emit(VerifyPinUiEvent.ShowLoading)
            try {
                val locationId = preferenceManager.getOccupiedLocationID()
                val response = repository.getUser(pin, locationId)
                if (response == null) {
                    _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                    _verifyPinUiEvent.emit(
                        VerifyPinUiEvent.ShowError("No user exist against this PIN")
                    )
                } else {
                    insertActiveUserDetails(response, pin)
                    _verifyPinUiEvent.emit(
                        VerifyPinUiEvent.GetActiveUserDetails(
                            repository.getActiveUserDetailsByPin(pin)
                        )
                    )
                    _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                    
                    if (repository.hasOpenBatch(
                            preferenceManager.getUserID(), preferenceManager.getStoreID(),
                            preferenceManager.getOccupiedRegisterID()
                        )
                    ) {
                        _verifyPinUiEvent.emit(VerifyPinUiEvent.NavigateToCartScreen)
                    } else {
                        _verifyPinUiEvent.emit(VerifyPinUiEvent.NavigateToCashDenomination)
                    }
                }

            } catch (e: Exception) {
                _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                _verifyPinUiEvent.emit(
                    VerifyPinUiEvent.ShowError(e.message ?: "Something went wrong")
                )
            }
        }
    }

    suspend fun insertActiveUserDetails(allStoreUsers: AllStoreUsers, pin: String) {
        val user = allStoreUsers
        val store = repository.getStore()
        val location = repository.getLocationByLocationID(preferenceManager.getOccupiedLocationID())
        val register = repository.getRegisterByRegisterID(preferenceManager.getOccupiedRegisterID())
        preferenceManager.setUserID(user.id)

        val activeUserDetails = ActiveUserDetails(
            id = user.id,
            name = user.name,
            email = user.email,
            username = user.username,
            password = user.password,
            pin = user.pin,
            userStatus = user.status,
            phoneNo = user.phoneNo,
            storeId = user.storeId,
            locationId = location.id,
            roleId = user.roleId,
            roleTitle = user.roleTitle,
            storeName = store.name,
            address = store.address,
            storeMultiCashier = store.multiCashier,
            policy = store.policy,
            advertisementImg = store.advertisementImg,
            isAdvertisement = store.isAdvertisement,
            alt = store.logoUrl?.alt,
            original = store.logoUrl?.original,
            thumbnail = store.logoUrl?.thumbnail,
            locationName = location.name,
            locationAddress = location.address,
            locationStatus = location.status,
            zipCode = location.zipCode,
            taxValue = location.taxValue,
            taxTitle = location.taxTitle,
            startTime = location.startTime,
            endTime = location.endTime,
            locationMultiCashier = location.multiCashier,
            registerId = register.id,
            registerName = register.name,
            registerStatus = register.status
        )
        repository.insertActiveUserDetailsIntoLocalDB(activeUserDetails)
    }
}