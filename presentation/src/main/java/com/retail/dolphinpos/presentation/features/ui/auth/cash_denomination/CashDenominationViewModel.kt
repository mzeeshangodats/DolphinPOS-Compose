package com.retail.dolphinpos.presentation.features.ui.auth.cash_denomination

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.Denomination
import com.retail.dolphinpos.domain.model.auth.cash_denomination.DenominationType
import com.retail.dolphinpos.domain.repositories.auth.CashDenominationRepository
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.Loader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class CashDenominationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CashDenominationRepository,
    private val preferenceManager: PreferenceManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _denominations = MutableStateFlow<List<Denomination>>(emptyList())
    val denominations: StateFlow<List<Denomination>> = _denominations.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    private val _selectedDenomination = MutableStateFlow<Denomination?>(null)
    val selectedDenomination: StateFlow<Denomination?> = _selectedDenomination.asStateFlow()

    private val _currentCount = MutableStateFlow("0")
    val currentCount: StateFlow<String> = _currentCount.asStateFlow()

    private val _cashDenominationUiEvent = MutableSharedFlow<CashDenominationUiEvent>()
    val cashDenominationUiEvent: SharedFlow<CashDenominationUiEvent> = _cashDenominationUiEvent.asSharedFlow()

    init {
        initializeDenominations()
    }

    private fun initializeDenominations() {
        val denominationsList = listOf(
            // Cash denominations
            Denomination(100.0, 0, "$100", DenominationType.CASH),
            Denomination(50.0, 0, "$50", DenominationType.CASH),
            Denomination(20.0, 0, "$20", DenominationType.CASH),
            Denomination(10.0, 0, "$10", DenominationType.CASH),
            Denomination(5.0, 0, "$5", DenominationType.CASH),
            Denomination(1.0, 0, "$1", DenominationType.CASH),

            // Coin denominations
            Denomination(0.25, 0, "$0.25", DenominationType.COIN),
            Denomination(0.10, 0, "$0.10", DenominationType.COIN),
            Denomination(0.05, 0, "$0.05", DenominationType.COIN),
            Denomination(0.01, 0, "$0.01", DenominationType.COIN)
        )

        _denominations.value = denominationsList
        calculateTotal()
    }

    fun selectDenomination(denomination: Denomination) {
        _selectedDenomination.value = denomination
        _currentCount.value = denomination.count.toString()
    }

    fun updateCount(count: Int) {
        val selected = _selectedDenomination.value ?: return
        val updatedDenominations = _denominations.value.toMutableList()
        val index = updatedDenominations.indexOfFirst { it.value == selected.value }

        if (index != -1) {
            updatedDenominations[index] = selected.updateCount(count)
            _denominations.value = updatedDenominations
            _selectedDenomination.value = updatedDenominations[index]
            calculateTotal()
        }
    }

    fun addDigit(digit: String) {
        val currentCountValue = _currentCount.value
        val newCount = if (currentCountValue == "0") digit else currentCountValue + digit
        
        // Restrict to maximum 10 digits
        if (newCount.length <= 10) {
            // Convert to Long first to handle large numbers, then cap at Int.MAX_VALUE
            val longValue = newCount.toLongOrNull()
            if (longValue != null && longValue <= Int.MAX_VALUE) {
                _currentCount.value = newCount
                updateCount(longValue.toInt())
            }
            // If the value exceeds Int.MAX_VALUE, keep the previous count and don't update
        }
    }

    fun clearCount() {
        _currentCount.value = "0"
        updateCount(0)
    }

    fun addDoubleZero() {
        val currentCountValue = _currentCount.value
        val newCount = if (currentCountValue == "0") "00" else currentCountValue + "00"
        
        // Restrict to maximum 10 digits
        if (newCount.length <= 10) {
            // Convert to Long first to handle large numbers, then cap at Int.MAX_VALUE
            val longValue = newCount.toLongOrNull()
            if (longValue != null && longValue <= Int.MAX_VALUE) {
                _currentCount.value = newCount
                updateCount(longValue.toInt())
            }
            // If the value exceeds Int.MAX_VALUE, keep the previous count and don't update
        }
    }

    private fun calculateTotal() {
        val total = _denominations.value.sumOf { it.subtotal }
        _totalAmount.value = total
    }

    fun resetAllDenominations() {
        val resetDenominations = _denominations.value.map { it.updateCount(0) }
        _denominations.value = resetDenominations
        _currentCount.value = "0"
        _selectedDenomination.value = null
        calculateTotal()
    }

    fun startBatch(batchNo: String) {
        viewModelScope.launch {
            try {
                // Show progress dialog
                Loader.show("Starting batch...")
                
                // Batch/open API requires internet connection - check before proceeding
                if (!networkMonitor.isNetworkAvailable()) {
                    Loader.hide()
                    _cashDenominationUiEvent.emit(
                        CashDenominationUiEvent.ShowNoInternetDialog(context.getString(R.string.no_internet_connection))
                    )
                    return@launch
                }
                
                // Save batch number to SharedPreferences
                preferenceManager.setBatchNo(batchNo)
                // Set batch status to "open"
                preferenceManager.setBatchStatus("active")
                
                val batch = Batch(
                    batchNo = batchNo,
                    userId = preferenceManager.getUserID(),
                    storeId = preferenceManager.getStoreID(),
                    registerId = preferenceManager.getOccupiedRegisterID(),
                    locationId = preferenceManager.getOccupiedLocationID(),
                    startingCashAmount = totalAmount.value
                )

                // Save to local database after internet check
                repository.insertBatchIntoLocalDB(batch)

                Log.e("Batch", "Started")
                
                // Internet is available, proceed with API call
                try {
                    val batchOpenRequest = BatchOpenRequest(
                        batchNo = batchNo,
                        storeId = preferenceManager.getStoreID(),
                        userId = preferenceManager.getUserID(),
                        locationId = preferenceManager.getOccupiedLocationID(),
                        storeRegisterId = preferenceManager.getOccupiedRegisterID(),
                        startingCashAmount = totalAmount.value
                    )

                    repository.batchOpen(batchOpenRequest).onSuccess { response ->
                        // Check if response message indicates batch is already active
                        val responseMessage = response.message ?: ""
                        if (responseMessage.contains("Batch already active.", ignoreCase = true)) {
                            Log.e("Batch", "Batch already active, navigating to home")
                            // Hide progress dialog
                            Loader.hide()
                            // Navigate to home since batch is already active
                            _cashDenominationUiEvent.emit(CashDenominationUiEvent.NavigateToHome)
                        } else {
                            Log.e("Batch", "Batch successfully synced with server")
                            // Mark batch as synced in database
                            repository.markBatchAsSynced(batchNo)
                            // Hide progress dialog
                            Loader.hide()
                            // Emit success event to navigate to home
                            _cashDenominationUiEvent.emit(CashDenominationUiEvent.NavigateToHome)
                        }
                    }.onFailure { e ->
                        Log.e("Batch", "Failed to sync batch with server: ${e.message}")
                        // Hide progress dialog
                        Loader.hide()
                        
                        // Check if error is 422 status code
                        val cause = e.cause
                        if (cause is HttpException && cause.code() == 422) {
                            // 422 status code - show error and do NOT navigate to home
                            val errorMessage = e.message ?: "Validation error. Please check your input."
                            Log.e("Batch", "422 error: $errorMessage")
                            _cashDenominationUiEvent.emit(
                                CashDenominationUiEvent.ShowError(errorMessage)
                            )
                            return@launch
                        }
                        
                        // Check if error message indicates batch is already active
                        val errorMessage = e.message ?: "Failed to sync batch"
                        if (errorMessage.contains("Batch already active.", ignoreCase = true)) {
                            // Batch is already active, navigate to home
                            _cashDenominationUiEvent.emit(CashDenominationUiEvent.NavigateToHome)
                        } else {
                            // Show error message from server in dialog
                            _cashDenominationUiEvent.emit(
                                CashDenominationUiEvent.ShowError(errorMessage)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Batch", "Failed to sync batch with server: ${e.message}")
                    // Hide progress dialog
                    Loader.hide()
                    
                    // Check if error is 422 status code
                    if (e is HttpException && e.code() == 422) {
                        // 422 status code - show error and do NOT navigate to home
                        val errorMessage = e.message ?: "Validation error. Please check your input."
                        Log.e("Batch", "422 error: $errorMessage")
                        _cashDenominationUiEvent.emit(
                            CashDenominationUiEvent.ShowError(errorMessage)
                        )
                        return@launch
                    }
                    
                    // Emit error event with server error message
                    _cashDenominationUiEvent.emit(
                        CashDenominationUiEvent.ShowError(e.message ?: "Failed to sync batch")
                    )
                }
            } catch (e: Exception) {
                // Hide progress dialog on any unexpected error
                Loader.hide()
                Log.e("Batch", "Unexpected error: ${e.message}")
                
                // Check if error is 422 status code
                if (e is HttpException && e.code() == 422) {
                    // 422 status code - show error and do NOT navigate to home
                    val errorMessage = e.message ?: "Validation error. Please check your input."
                    Log.e("Batch", "422 error: $errorMessage")
                    _cashDenominationUiEvent.emit(
                        CashDenominationUiEvent.ShowError(errorMessage)
                    )
                    return@launch
                }
                
                _cashDenominationUiEvent.emit(
                    CashDenominationUiEvent.ShowError(e.message ?: "Failed to start batch")
                )
            }
        }
    }

    fun generateBatchNo(): String {
        val storeId = preferenceManager.getStoreID()
        val locationId = preferenceManager.getOccupiedLocationID()
        val registerId = preferenceManager.getOccupiedRegisterID()
        val userId = preferenceManager.getUserID()
        val epochMillis = System.currentTimeMillis()
        
        return "BATCH_S${storeId}L${locationId}R${registerId}U${userId}-$epochMillis"
    }
}

