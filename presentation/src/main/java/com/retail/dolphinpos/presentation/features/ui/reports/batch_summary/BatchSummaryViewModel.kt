package com.retail.dolphinpos.presentation.features.ui.reports.batch_summary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.domain.model.report.batch_report.BatchReportData
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

sealed class BatchSummaryUiEvent {
    object ShowLoading : BatchSummaryUiEvent()
    object HideLoading : BatchSummaryUiEvent()
    data class ShowError(val message: String) : BatchSummaryUiEvent()
}

@HiltViewModel
class BatchSummaryViewModel @Inject constructor(
    private val batchReportRepository: BatchReportRepository
) : ViewModel() {

    private val _batchReport = MutableStateFlow<BatchReportData?>(null)
    val batchReport: StateFlow<BatchReportData?> = _batchReport.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<BatchSummaryUiEvent>()
    val uiEvent: SharedFlow<BatchSummaryUiEvent> = _uiEvent.asSharedFlow()

    companion object {
        private const val TAG = "BatchSummaryViewModel"
    }

    fun loadBatchReport(batchNo: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiEvent.emit(BatchSummaryUiEvent.ShowLoading)

                if (batchNo.isEmpty()) {
                    _isLoading.value = false
                    _uiEvent.emit(BatchSummaryUiEvent.HideLoading)
                    _uiEvent.emit(
                        BatchSummaryUiEvent.ShowError("No batch number provided")
                    )
                    return@launch
                }

                // Fetch batch report from API
                val batchReportResponse = batchReportRepository.getBatchReport(batchNo)

                _batchReport.value = batchReportResponse.data

                _isLoading.value = false
                _uiEvent.emit(BatchSummaryUiEvent.HideLoading)
            } catch (e: HttpException) {
                _isLoading.value = false
                _uiEvent.emit(BatchSummaryUiEvent.HideLoading)
                
                if (e.code() == 404) {
                    _uiEvent.emit(
                        BatchSummaryUiEvent.ShowError("Batch report not found")
                    )
                } else {
                    _uiEvent.emit(
                        BatchSummaryUiEvent.ShowError(
                            e.message ?: "Failed to load batch report"
                        )
                    )
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _uiEvent.emit(BatchSummaryUiEvent.HideLoading)
                _uiEvent.emit(
                    BatchSummaryUiEvent.ShowError(
                        e.message ?: "Failed to load batch report"
                    )
                )
            }
        }
    }
}

