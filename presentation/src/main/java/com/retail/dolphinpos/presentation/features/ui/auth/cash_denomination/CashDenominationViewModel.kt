package com.retail.dolphinpos.presentation.features.ui.auth.cash_denomination

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.repositories.sync.PosSyncRepository
import com.retail.dolphinpos.domain.model.auth.cash_denomination.Denomination
import com.retail.dolphinpos.domain.model.auth.cash_denomination.DenominationType
import com.retail.dolphinpos.domain.usecases.sync.ScheduleSyncUseCase
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
import javax.inject.Inject

@HiltViewModel
class CashDenominationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val posSyncRepository: PosSyncRepository,
    private val scheduleSyncUseCase: ScheduleSyncUseCase,
    private val preferenceManager: PreferenceManager
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
                
                // Save batch number to SharedPreferences
                preferenceManager.setBatchNo(batchNo)
                // Set batch status to "open"
                preferenceManager.setBatchStatus("active")
                
                // Start batch using offline-first sync system
                posSyncRepository.startBatch(
                    batchId = batchNo,
                    userId = preferenceManager.getUserID(),
                    storeId = preferenceManager.getStoreID(),
                    registerId = preferenceManager.getOccupiedRegisterID(),
                    locationId = preferenceManager.getOccupiedLocationID(),
                    startingCashAmount = totalAmount.value
                )

                Log.d("Batch", "Batch started offline, queued for sync")

                // Schedule sync to process the OPEN_BATCH command
                scheduleSyncUseCase.scheduleSync(context)

                // Hide progress dialog
                Loader.hide()

                // Navigate to home immediately (batch is created locally)
                _cashDenominationUiEvent.emit(CashDenominationUiEvent.NavigateToHome)

            } catch (e: Exception) {
                // Hide progress dialog on any error
                Loader.hide()
                Log.e("Batch", "Error starting batch: ${e.message}", e)
                
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

