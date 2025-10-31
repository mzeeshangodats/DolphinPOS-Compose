package com.retail.dolphinpos.presentation.features.ui.auth.cash_denomination

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
class CashDenominationViewModel @Inject constructor(
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
            val batch = Batch(
                batchNo = batchNo,
                userId = preferenceManager.getUserID(),
                storeId = preferenceManager.getStoreID(),
                registerId = preferenceManager.getOccupiedRegisterID(),
                locationId = preferenceManager.getOccupiedLocationID(),
                startingCashAmount = totalAmount.value
            )

            // Always save to local database first
            repository.insertBatchIntoLocalDB(batch)

            Log.e("Batch", "Started")

            // If internet is available, call the API
            if (networkMonitor.isNetworkAvailable()) {
                try {
                    val batchOpenRequest = BatchOpenRequest(
                        batchNo = batchNo,
                        storeId = preferenceManager.getStoreID(),
                        userId = preferenceManager.getUserID(),
                        locationId = preferenceManager.getOccupiedLocationID(),
                        storeRegisterId = preferenceManager.getOccupiedRegisterID(),
                        startingCashAmount = totalAmount.value
                    )

                    repository.batchOpen(batchOpenRequest).onSuccess {
                        Log.e("Batch", "Batch successfully synced with server")
                        // Mark batch as synced in database
                        repository.markBatchAsSynced(batchNo)
                        // Emit success event to navigate to home
                        _cashDenominationUiEvent.emit(CashDenominationUiEvent.NavigateToHome)
                    }.onFailure { e ->
                        Log.e("Batch", "Failed to sync batch with server: ${e.message}")
                        // Show error message from server in dialog
                        _cashDenominationUiEvent.emit(
                            CashDenominationUiEvent.ShowError(e.message ?: "Failed to sync batch")
                        )
                    }
                } catch (e: Exception) {
                    Log.e("Batch", "Failed to sync batch with server: ${e.message}")
                    // Emit error event with server error message
                    _cashDenominationUiEvent.emit(
                        CashDenominationUiEvent.ShowError(e.message ?: "Failed to sync batch")
                    )
                }
            } else {
                Log.e("Batch", "No internet connection. Batch will be synced later via WorkManager")
                // No internet - proceed to home
                _cashDenominationUiEvent.emit(CashDenominationUiEvent.NavigateToHome)
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

