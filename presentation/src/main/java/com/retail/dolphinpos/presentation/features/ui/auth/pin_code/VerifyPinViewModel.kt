package com.retail.dolphinpos.presentation.features.ui.auth.pin_code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutRequest
import com.retail.dolphinpos.domain.usecases.GetCurrentTimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutHistoryData
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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

    private val _history = MutableStateFlow<List<ClockInOutHistoryData>>(emptyList())
    val history: StateFlow<List<ClockInOutHistoryData>> = _history

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
                        VerifyPinUiEvent.ShowDialog("No user exist against this PIN at this location")
                    )
                } else {
                    insertActiveUserDetails(response, pin)
                    _verifyPinUiEvent.emit(
                        VerifyPinUiEvent.GetActiveUserDetails(
                            repository.getActiveUserDetailsByPin(pin)
                        )
                    )
                    _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                    
                    // Check batch status from SharedPreferences
                    // If batch is closed, redirect to CashDenominationScreen
                    // Otherwise (batch is open), redirect to CartScreen (HomeScreen)
                    if (preferenceManager.isBatchOpen()) {
                        _verifyPinUiEvent.emit(VerifyPinUiEvent.NavigateToCartScreen)
                    } else {
                        _verifyPinUiEvent.emit(VerifyPinUiEvent.NavigateToCashDenomination)
                    }
                }

            } catch (e: Exception) {
                _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                _verifyPinUiEvent.emit(
                    VerifyPinUiEvent.ShowDialog(e.message ?: "Something went wrong")
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

    fun clockInOut(pin: String, slug: String) {
        viewModelScope.launch {
            _verifyPinUiEvent.emit(VerifyPinUiEvent.ShowLoading)
            try {
                val locationId = preferenceManager.getOccupiedLocationID()
                val user = repository.getUser(pin, locationId)
                if (user == null) {
                    _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                    _verifyPinUiEvent.emit(VerifyPinUiEvent.ShowDialog("Invalid PIN"))
                    return@launch
                }

                // Current UTC time in format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
                val utcTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now())

                val request = ClockInOutRequest(
                    slug = slug,
                    storeId = preferenceManager.getStoreID(),
                    time = utcTime,
                    userId = user.id
                )

                val result = repository.clockInOut(request)
                _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                result.fold(
                    onSuccess = { resp ->
                        // Toggle local status
                        if (slug == "check-in") {
                            preferenceManager.setClockInStatus(true)
                            preferenceManager.setClockInTime(System.currentTimeMillis())
                        } else {
                            preferenceManager.clockOut()
                        }
                        // Show message
                        _verifyPinUiEvent.emit(VerifyPinUiEvent.ShowDialog(resp.message, true))
                    },
                    onFailure = { err ->
                        val msg = err.message
                        if (msg == "OFFLINE_QUEUED") {
                            val action = if (slug == "check-in") "in" else "out"
                            // Set clock-in status even when offline (for check-in)
                            if (slug == "check-in") {
                                preferenceManager.setClockInStatus(true)
                                preferenceManager.setClockInTime(System.currentTimeMillis())
                            } else {
                                preferenceManager.clockOut()
                            }
                            _verifyPinUiEvent.emit(
                                VerifyPinUiEvent.ShowDialog(
                                    "Your clock $action request has been queued successfully. " +
                                            "It will be synchronized automatically when the internet connection is restored.",
                                    success = true
                                )
                            )
                        } else if (!msg.isNullOrBlank()) {
                            _verifyPinUiEvent.emit(VerifyPinUiEvent.ShowDialog(msg))
                        } else {
                            _verifyPinUiEvent.emit(VerifyPinUiEvent.ShowDialog("Clock In/Out failed"))
                        }
                    }
                )
            } catch (e: Exception) {
                _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                _verifyPinUiEvent.emit(VerifyPinUiEvent.ShowDialog(e.message ?: "Clock In/Out failed"))
            }
        }
    }

    fun getClockInOutHistory(pin: String){
        viewModelScope.launch {
            try {
                val locationId = preferenceManager.getOccupiedLocationID()
                val user = repository.getUser(pin, locationId)
                if (user == null) {
                    _history.value = emptyList()
                    return@launch
                }
                val result = repository.getClockInOutHistory(user.id)
                result.fold(
                    onSuccess = { list -> _history.value = list },
                    onFailure = { _history.value = emptyList() }
                )
            } catch (_: Exception) {
                _history.value = emptyList()
            }
        }
    }
}