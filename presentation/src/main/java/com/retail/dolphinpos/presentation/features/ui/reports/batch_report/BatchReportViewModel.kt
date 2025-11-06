package com.retail.dolphinpos.presentation.features.ui.reports.batch_report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.domain.model.auth.cash_denomination.AbandonCart
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.model.report.BatchReportData
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BatchReportUiEvent {
    object ShowLoading : BatchReportUiEvent()
    object HideLoading : BatchReportUiEvent()
    data class ShowError(val message: String) : BatchReportUiEvent()
    object NavigateToPinCode : BatchReportUiEvent()
}

@HiltViewModel
class BatchReportViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val preferenceManager: PreferenceManager,
    private val batchReportRepository: BatchReportRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _batchReport = MutableStateFlow<BatchReportData?>(null)
    val batchReport: StateFlow<BatchReportData?> = _batchReport.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showClosingCashDialog = MutableStateFlow(false)
    val showClosingCashDialog: StateFlow<Boolean> = _showClosingCashDialog.asStateFlow()

    private val _uiEvent = MutableSharedFlow<BatchReportUiEvent>()
    val uiEvent: SharedFlow<BatchReportUiEvent> = _uiEvent.asSharedFlow()

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
            } catch (e: Exception) {
                _isLoading.value = false
                _uiEvent.emit(BatchReportUiEvent.HideLoading)
                _uiEvent.emit(
                    BatchReportUiEvent.ShowError(
                        e.message ?: "Failed to load batch report"
                    )
                )
            }
        }
    }

    fun showClosingCashDialog() {
        _showClosingCashDialog.value = true
    }

    fun dismissClosingCashDialog() {
        _showClosingCashDialog.value = false
    }

    fun closeBatch(closingCashAmount: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiEvent.emit(BatchReportUiEvent.ShowLoading)

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
                _isLoading.value = false
                _uiEvent.emit(BatchReportUiEvent.HideLoading)
                _uiEvent.emit(BatchReportUiEvent.ShowError(e.message ?: "Failed to close batch"))
            }
        }
    }
}

