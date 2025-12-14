package com.retail.dolphinpos.presentation.features.ui.auth.pin_code

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutHistoryData
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutRequest
import com.retail.dolphinpos.domain.model.auth.login.response.AllStoreUsers
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.model.auth.select_registers.request.VerifyRegisterRequest
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import com.retail.dolphinpos.domain.repositories.auth.VerifyPinRepository
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import com.retail.dolphinpos.domain.usecases.GetCurrentTimeUseCase
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.common.network.NoConnectivityException
import com.retail.dolphinpos.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class VerifyPinViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VerifyPinRepository,
    private val preferenceManager: PreferenceManager,
    private val getCurrentTimeUseCase: GetCurrentTimeUseCase,
    private val batchReportRepository: BatchReportRepository,
    private val networkMonitor: NetworkMonitor,
    private val storeRegistersRepository: StoreRegistersRepository
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

                    // Check internet before batch status API call
                    if (!networkMonitor.isNetworkAvailable()) {
                        _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                        _verifyPinUiEvent.emit(
                            VerifyPinUiEvent.ShowNoInternetDialog(context.getString(R.string.no_internet_connection))
                        )
                        return@launch
                    }

                    val batchReport = checkBatchStatus()
                    val batchStatus = batchReport?.data?.status
                    batchStatus?.let { preferenceManager.setBatchStatus(it) }

                    // If batch status is closed, show dialog then navigate to cash denomination screen
                    if (batchStatus?.equals("closed", ignoreCase = true) == true) {
                        preferenceManager.clearBatchNo()
                        _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                        _verifyPinUiEvent.emit(
                            VerifyPinUiEvent.ShowBatchClosedDialog("Your batch has been closed by someone from portal")
                        )
                        return@launch
                    }

                    // Verify register status after batch status check
                    val storeId = preferenceManager.getStoreID()
                    val locationId = preferenceManager.getOccupiedLocationID()
                    val storeRegisterId = preferenceManager.getOccupiedRegisterID()

                    if (storeId != 0 && locationId != 0 && storeRegisterId != 0) {
                        try {
                            val verifyRequest = VerifyRegisterRequest(
                                storeId = storeId,
                                locationId = locationId,
                                storeRegisterId = storeRegisterId
                            )

                            val verifyResponse =
                                storeRegistersRepository.verifyStoreRegister(verifyRequest)
                            val registerStatus = verifyResponse.data.status.lowercase()

                            // Check if register is NOT occupied (active/free)
                            if (registerStatus != "occupied") {
                                Log.w(TAG, "Register status is not occupied: $registerStatus")

                                // Close batch if there's an open batch
                                val batchNo = preferenceManager.getBatchNo()
                                try {
                                    val batchDetails = batchReportRepository.getBatchDetails()
                                    val userId = preferenceManager.getUserID()
                                    val closingCashAmount = batchDetails.startingCashAmount

                                    val batchCloseRequest = BatchCloseRequest(
                                        cashierId = userId,
                                        closedBy = userId,
                                        closingCashAmount = closingCashAmount,
                                        locationId = locationId,
                                        orders = emptyList(),
                                        paxBatchNo = "",
                                        storeId = storeId
                                    )

                                    val closeResult =
                                        batchReportRepository.batchClose(batchNo, batchCloseRequest)

                                    // Properly handle the Result
                                    if (closeResult.isSuccess) {
                                        closeResult.getOrNull()?.let {
                                            Log.d(
                                                TAG,
                                                "Batch closed successfully after register release"
                                            )
                                            preferenceManager.setBatchStatus("closed")
                                            preferenceManager.clearBatchNo()
                                        }
                                    } else {
                                        val exception = closeResult.exceptionOrNull()
                                        Log.e(
                                            TAG,
                                            "Failed to close batch: ${exception?.message ?: "Unknown error"}"
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error closing batch: ${e.message}", e)
                                }

                                preferenceManager.setRegister(false)
                                _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)
                                _verifyPinUiEvent.emit(
                                    VerifyPinUiEvent.ShowRegisterReleasedDialog("Your register has been release by someone from portal")
                                )
                                return@launch
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error verifying register status: ${e.message}", e)
                            // Continue with normal flow if verification fails
                        }
                    }

                    _verifyPinUiEvent.emit(VerifyPinUiEvent.HideLoading)

                    // Determine navigation based on remote batch status
                    when {
                        // If batch is already active (open), proceed to home
                        batchStatus?.equals("active", ignoreCase = true) == true -> {
                            _verifyPinUiEvent.emit(VerifyPinUiEvent.NavigateToCartScreen)
                        }
                        // If batch status is closed, redirect to Cash Denomination to start a new batch
                        batchStatus?.equals("closed", ignoreCase = true) == true -> {
                            _verifyPinUiEvent.emit(VerifyPinUiEvent.NavigateToCashDenomination)
                        }
                        // If remote status is null, check local preference
                        else -> {
                            val isBatchOpen = preferenceManager.isBatchOpen()
                            if (isBatchOpen) {
                                _verifyPinUiEvent.emit(VerifyPinUiEvent.NavigateToCartScreen)
                            } else {
                                _verifyPinUiEvent.emit(VerifyPinUiEvent.NavigateToCashDenomination)
                            }
                        }
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
            taxTitle = location.taxTitle,
            startTime = location.startTime,
            endTime = location.endTime,
            locationMultiCashier = location.multiCashier,
            dualPricePercentage = location.dualPricePercentage?.toString(),
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
                _verifyPinUiEvent.emit(
                    VerifyPinUiEvent.ShowDialog(
                        e.message ?: "Clock In/Out failed"
                    )
                )
            }
        }
    }

    fun getClockInOutHistory(pin: String) {
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

    private suspend fun checkBatchStatus(): com.retail.dolphinpos.domain.model.report.BatchReport? {
        val batchNo = preferenceManager.getBatchNo()
        if (batchNo.isBlank()) {
            return null
        }
        // Internet check is already done before calling this function
        return try {
            batchReportRepository.getBatchReport(batchNo)
        } catch (e: NoConnectivityException) {
            Log.e(TAG, "No internet connection: ${e.message}")
            throw e // Re-throw to be caught by caller
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch batch status for $batchNo: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "VerifyPinViewModel"
    }
}