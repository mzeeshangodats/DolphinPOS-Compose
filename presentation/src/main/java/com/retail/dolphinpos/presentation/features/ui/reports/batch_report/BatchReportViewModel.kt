package com.retail.dolphinpos.presentation.features.ui.reports.batch_report

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.model.report.batch_report.BatchReportData
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.CloseBatchUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.InitializeTerminalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import javax.inject.Inject

sealed class BatchReportUiEvent {
    object ShowLoading : BatchReportUiEvent()
    object HideLoading : BatchReportUiEvent()
    data class ShowError(val message: String) : BatchReportUiEvent()
    object NavigateToPinCode : BatchReportUiEvent()
    object NavigateToCashDenomination : BatchReportUiEvent()
}

@HiltViewModel
class BatchReportViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val preferenceManager: PreferenceManager,
    private val batchReportRepository: BatchReportRepository,
    private val userDao: UserDao,
    private val initializeTerminalUseCase: InitializeTerminalUseCase,
    private val closeBatchUseCase: CloseBatchUseCase
) : ViewModel() {

    private val _batchReport = MutableStateFlow<BatchReportData?>(null)
    val batchReport: StateFlow<BatchReportData?> = _batchReport.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showClosingCashDialog = MutableStateFlow(false)
    val showClosingCashDialog: StateFlow<Boolean> = _showClosingCashDialog.asStateFlow()

    private val _uiEvent = MutableSharedFlow<BatchReportUiEvent>()
    val uiEvent: SharedFlow<BatchReportUiEvent> = _uiEvent.asSharedFlow()

    private var currentTerminalSessionId: String? = null

    companion object {
        private const val TAG = "BatchReportViewModel"
    }

    fun loadBatchReport() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiEvent.emit(BatchReportUiEvent.ShowLoading)

                // Get current batch number from SharedPreferences
                val batchNo = preferenceManager.getBatchNo()
                if (batchNo.isEmpty()) {
                    _isLoading.value = false
                    _uiEvent.emit(BatchReportUiEvent.HideLoading)
                    _uiEvent.emit(
                        BatchReportUiEvent.ShowError("No batch number found")
                    )
                    return@launch
                }

                // Fetch batch report from API
                val batchReportResponse = batchReportRepository.getBatchReport(batchNo)

                _batchReport.value = batchReportResponse.data

                _isLoading.value = false
                _uiEvent.emit(BatchReportUiEvent.HideLoading)
            } catch (e: retrofit2.HttpException) {
                _isLoading.value = false
                _uiEvent.emit(BatchReportUiEvent.HideLoading)
                
                // Handle 404 error - redirect to CashDenominationScreen
                if (e.code() == 404) {
                    _uiEvent.emit(BatchReportUiEvent.NavigateToCashDenomination)
                } else {
                    _uiEvent.emit(
                        BatchReportUiEvent.ShowError(
                            e.message ?: "Failed to load batch report"
                        )
                    )
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _uiEvent.emit(BatchReportUiEvent.HideLoading)
                
                // Check if the cause is HttpException with 404
                val cause = e.cause
                if (cause is retrofit2.HttpException && cause.code() == 404) {
                    _uiEvent.emit(BatchReportUiEvent.NavigateToCashDenomination)
                } else {
                    _uiEvent.emit(
                        BatchReportUiEvent.ShowError(
                            e.message ?: "Failed to load batch report"
                        )
                    )
                }
            }
        }
    }

    fun showClosingCashDialog() {
        _showClosingCashDialog.value = true
    }

    fun dismissClosingCashDialog() {
        _showClosingCashDialog.value = false
    }

    fun closeBatch(closingCashAmount: Double, shouldClosePaxBatch: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _uiEvent.emit(BatchReportUiEvent.ShowLoading)

                // Step 1: Close PAX Batch if checkbox is checked (BEFORE regular batch close)
                if (shouldClosePaxBatch) {
                    Log.d(TAG, "closeBatch: PAX batch close requested, initializing terminal...")
                    val paxResult = closePaxBatch()
                    
                    if (!paxResult.first) {
                        // PAX batch close failed - stop the process and show error
                        _isLoading.value = false
                        _uiEvent.emit(BatchReportUiEvent.HideLoading)
                        val errorMessage = paxResult.second ?: "Failed to close PAX batch. Please try again."
                        _uiEvent.emit(BatchReportUiEvent.ShowError(errorMessage))
                        return@launch
                    }
                    Log.d(TAG, "closeBatch: PAX batch closed successfully, proceeding with regular batch close")
                }

                // Step 2: Close Regular Batch
                // Get batch number from SharedPreferences
                val batchNo = preferenceManager.getBatchNo()
                if (batchNo.isEmpty()) {
                    _isLoading.value = false
                    _uiEvent.emit(BatchReportUiEvent.HideLoading)
                    _uiEvent.emit(
                        BatchReportUiEvent.ShowError("No batch number found")
                    )
                    return@launch
                }

                // Get current batch details from database
                val batchEntity = userDao.getBatchDetails()

                // Get required IDs
                val userId = preferenceManager.getUserID()
                val storeId = preferenceManager.getStoreID()
                val locationId = preferenceManager.getOccupiedLocationID()

                val batchCloseRequest = BatchCloseRequest(
                    cashierId = userId,
                    closedBy = userId,
                    closingCashAmount = closingCashAmount,
                    locationId = locationId,
                    orders = emptyList(),
                    paxBatchNo = "",
                    storeId = storeId
                )

                val result =
                    batchReportRepository.batchClose(batchNo, batchCloseRequest)

                result.onSuccess { response ->
                    // Update local batch entity
                    val updatedBatch = batchEntity.copy(
                        closedAt = System.currentTimeMillis(),
                        closingCashAmount = closingCashAmount
                    )
                    userDao.updateBatch(updatedBatch)

                    // Set batch status to "closed" in SharedPreferences
                    preferenceManager.setBatchStatus("closed")

                    _isLoading.value = false
                    _uiEvent.emit(BatchReportUiEvent.HideLoading)

                    // Dismiss dialog
                    _showClosingCashDialog.value = false

                    // Navigate to PinCode after successful batch close
                    _uiEvent.emit(BatchReportUiEvent.NavigateToPinCode)
                }.onFailure { exception ->
                    _isLoading.value = false
                    _uiEvent.emit(BatchReportUiEvent.HideLoading)
                    _uiEvent.emit(
                        BatchReportUiEvent.ShowError(
                            exception.message ?: "Failed to close batch"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "closeBatch: Error during batch close", e)
                _isLoading.value = false
                _uiEvent.emit(BatchReportUiEvent.HideLoading)
                _uiEvent.emit(BatchReportUiEvent.ShowError(e.message ?: "Failed to close batch"))
            }
        }
    }

    /**
     * Closes PAX batch. Returns Pair<Boolean, String?> where Boolean indicates success
     * and String contains error message if failed, null if successful.
     */
    private suspend fun closePaxBatch(): Pair<Boolean, String?> {
        return withContext(Dispatchers.IO) {
            try {
                // If no session exists, initialize terminal first
                if (currentTerminalSessionId == null) {
                    Log.d(TAG, "closePaxBatch: No terminal session found. Initializing terminal...")
                    
                    val initResult = suspendCancellableCoroutine<Pair<Boolean, String?>> { continuation ->
                        // Launch coroutine to call suspend function
                        viewModelScope.launch(Dispatchers.IO) {
                            initializeTerminalUseCase { result ->
                                val session = result.session
                                if (result.isSuccess && session != null) {
                                    currentTerminalSessionId = session.sessionId
                                    Log.d(TAG, "closePaxBatch: Terminal initialized, sessionId: ${currentTerminalSessionId}")
                                    continuation.resume(Pair(true, null))
                                } else {
                                    val errorMsg = result.message ?: "Unable to communicate with Terminal. Please make sure terminal is connected to same network as POS"
                                    Log.e(TAG, "closePaxBatch: Terminal initialization failed - $errorMsg")
                                    continuation.resume(Pair(false, errorMsg))
                                }
                            }
                        }
                    }
                    
                    if (!initResult.first || currentTerminalSessionId == null) {
                        val errorMsg = initResult.second ?: "Failed to initialize terminal"
                        Log.e(TAG, "closePaxBatch: Failed to initialize terminal - $errorMsg")
                        return@withContext Pair(false, errorMsg)
                    }
                }

                // Close PAX batch using the session
                Log.d(TAG, "closePaxBatch: Calling closeBatchUseCase with sessionId: $currentTerminalSessionId")
                
                val closeResult = suspendCancellableCoroutine<Pair<Boolean, String?>> { continuation ->
                    // Launch coroutine to call suspend function
                    viewModelScope.launch(Dispatchers.IO) {
                        closeBatchUseCase(currentTerminalSessionId) { result ->
                            if (result.isSuccess) {
                                Log.d(TAG, "closePaxBatch: PAX batch closed successfully - ${result.message}")
                                continuation.resume(Pair(true, null))
                            } else {
                                val errorMsg = result.message ?: "Failed to close PAX batch"
                                Log.e(TAG, "closePaxBatch: PAX batch close failed - $errorMsg")
                                continuation.resume(Pair(false, errorMsg))
                            }
                        }
                    }
                }

                if (!closeResult.first) {
                    val errorMsg = closeResult.second ?: "PAX batch close failed"
                    Log.e(TAG, "closePaxBatch: PAX batch close failed - $errorMsg")
                    return@withContext Pair(false, errorMsg)
                }

                Pair(true, null)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Exception during PAX batch close"
                Log.e(TAG, "closePaxBatch: Exception during PAX batch close", e)
                Pair(false, errorMsg)
            }
        }
    }
}

