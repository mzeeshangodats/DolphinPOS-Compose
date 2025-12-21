package com.retail.dolphinpos.presentation.features.ui.auth.cash_denomination

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.network.NoConnectivityException
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.auth.cash_denomination.Denomination
import com.retail.dolphinpos.domain.model.auth.cash_denomination.DenominationType
import com.retail.dolphinpos.domain.repositories.auth.CashDenominationRepository
import com.retail.dolphinpos.domain.repositories.batch.BatchRepository
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
    private val repository: CashDenominationRepository,
    private val batchRepository: BatchRepository,
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
                
                val userId = preferenceManager.getUserID()
                val storeId = preferenceManager.getStoreID()
                val registerId = preferenceManager.getOccupiedRegisterID()
                val locationId = preferenceManager.getOccupiedLocationID()
                val startingCashAmount = totalAmount.value
                
                // Validate required fields
                if (registerId == 0 || locationId == 0) {
                    Loader.hide()
                    _cashDenominationUiEvent.emit(
                        CashDenominationUiEvent.ShowError("Invalid register or location. Please try again.")
                    )
                    return@launch
                }
                
                // Start batch locally (works offline immediately)
                // This creates the batch immediately and schedules sync via WorkManager
                val batch = batchRepository.startBatch(
                    userId = userId,
                    storeId = storeId,
                    registerId = registerId,
                    locationId = locationId,
                    startingCashAmount = startingCashAmount,
                    batchNo = batchNo
                )
                
                // Save batch number to SharedPreferences for backward compatibility
                preferenceManager.setBatchNo(batch.batchNo)
                // Set batch status to "active"
                preferenceManager.setBatchStatus("active")
                
                Log.d("Batch", "Batch started locally with UUID: ${batch.batchId}, batchNo: ${batch.batchNo}")
                
                // Hide progress dialog
                Loader.hide()
                
                // Batch is now active locally - navigate to home
                // Sync to backend will happen automatically via WorkManager when network is available
                _cashDenominationUiEvent.emit(CashDenominationUiEvent.NavigateToHome)
            } catch (e: NoConnectivityException) {
                // This should NOT happen during batch start as it's offline-first
                // Log it for debugging but still proceed - batch was already created locally
                Loader.hide()
                Log.w("Batch", "NoConnectivityException during batch start (unexpected): ${e.message}", e)
                // Batch was already created locally, so we can still navigate
                // If there's a real issue, it would have failed before this point
                _cashDenominationUiEvent.emit(CashDenominationUiEvent.NavigateToHome)
            } catch (e: IllegalStateException) {
                // Handle case where no active batch exists or other state errors
                Loader.hide()
                Log.e("Batch", "Failed to start batch: ${e.message}")
                _cashDenominationUiEvent.emit(
                    CashDenominationUiEvent.ShowError(e.message ?: "Failed to start batch")
                )
            } catch (e: Exception) {
                // Hide progress dialog on any unexpected error
                Loader.hide()
                Log.e("Batch", "Unexpected error: ${e.message}", e)
                // Don't show network-related error messages for offline operations
                val errorMessage = if (e.message?.contains("internet", ignoreCase = true) == true ||
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("connectivity", ignoreCase = true) == true) {
                    "Failed to start batch. Please try again."
                } else {
                    e.message ?: "Failed to start batch"
                }
                _cashDenominationUiEvent.emit(
                    CashDenominationUiEvent.ShowError(errorMessage)
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

