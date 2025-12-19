package com.retail.dolphinpos.presentation.features.ui.reports.batch_history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.report.batch_history.BatchReportHistoryData
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import com.retail.dolphinpos.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

sealed class BatchHistoryUiEvent {
    object ShowLoading : BatchHistoryUiEvent()
    object HideLoading : BatchHistoryUiEvent()
    data class ShowError(val message: String) : BatchHistoryUiEvent()
    data class ShowNoInternetDialog(val message: String) : BatchHistoryUiEvent()
}

@HiltViewModel
class BatchHistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batchReportRepository: BatchReportRepository,
    private val preferenceManager: PreferenceManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _batches = MutableStateFlow<List<BatchReportHistoryData>>(emptyList())
    val batches: StateFlow<List<BatchReportHistoryData>> = _batches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<BatchHistoryUiEvent>()
    val uiEvent: SharedFlow<BatchHistoryUiEvent> = _uiEvent.asSharedFlow()

    private val _startDate = MutableStateFlow<String?>(null)
    val startDate: StateFlow<String?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<String?>(null)
    val endDate: StateFlow<String?> = _endDate.asStateFlow()

    fun setStartDate(date: String) {
        _startDate.value = date
    }

    fun setEndDate(date: String) {
        _endDate.value = date
    }

    fun loadBatchHistory(keyword: String? = null, reset: Boolean = true) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiEvent.emit(BatchHistoryUiEvent.ShowLoading)

                // Clear batches when resetting to ensure clean refresh
                if (reset) {
                    _batches.value = emptyList()
                }

                // Check internet connection
                if (!networkMonitor.isNetworkAvailable()) {
                    _isLoading.value = false
                    _uiEvent.emit(BatchHistoryUiEvent.HideLoading)
                    _uiEvent.emit(
                        BatchHistoryUiEvent.ShowNoInternetDialog(context.getString(R.string.no_internet_connection))
                    )
                    // Still load from local database for offline functionality
                    loadBatchHistoryFromLocal(keyword)
                    return@launch
                }

                // If internet is available, fetch from API and save to local DB
                loadBatchHistoryFromApi(keyword)

                // Always load from local database (works for both online and offline)
                loadBatchHistoryFromLocal(keyword)
            } catch (e: Exception) {
                _isLoading.value = false
                _uiEvent.emit(BatchHistoryUiEvent.HideLoading)
                _uiEvent.emit(BatchHistoryUiEvent.ShowError(e.message ?: "Failed to load batch history"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadBatchHistoryFromApi(keyword: String?) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()

            // Use selected dates, or default to last 1 day
            val endDateStr = _endDate.value ?: run {
                dateFormat.format(calendar.time)
            }

            val startDateStr = _startDate.value ?: run {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                dateFormat.format(calendar.time)
            }

            val storeId = preferenceManager.getStoreID()

            val result = batchReportRepository.getBatchHistory(
                startDate = startDateStr,
                endDate = endDateStr,
                status = "closed",
                storeId = storeId,
                page = 1,
                limit = 10,
                paginate = true,
                orderBy = "createdAt",
                order = "desc",
                keyword = keyword
            )

            result.onSuccess {
                // Batch history is saved to local DB in repository
                android.util.Log.d("BatchHistoryViewModel", "Batch history loaded from API: ${it.size} items")
            }.onFailure { e ->
                android.util.Log.e("BatchHistoryViewModel", "Failed to fetch batch history from API: ${e.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("BatchHistoryViewModel", "Error loading batch history from API: ${e.message}")
        }
    }

    private suspend fun loadBatchHistoryFromLocal(keyword: String?) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()

            // Use selected dates, or default to last 1 day
            val endDateStr = _endDate.value ?: run {
                dateFormat.format(calendar.time)
            }

            val startDateStr = _startDate.value ?: run {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                dateFormat.format(calendar.time)
            }

            val storeId = preferenceManager.getStoreID()

            val result = batchReportRepository.getBatchHistory(
                startDate = startDateStr,
                endDate = endDateStr,
                status = "closed",
                storeId = storeId,
                page = 1,
                limit = 100,
                paginate = true,
                orderBy = "createdAt",
                order = "desc",
                keyword = keyword
            )

            result.onSuccess { batchHistoryList ->
                // Filter by keyword if provided
                val filteredList = if (keyword.isNullOrBlank()) {
                    batchHistoryList
                } else {
                    batchHistoryList.filter {
                        it.batchNo.contains(keyword, ignoreCase = true) ||
                        it.status.contains(keyword, ignoreCase = true)
                    }
                }

                _batches.value = filteredList
                _uiEvent.emit(BatchHistoryUiEvent.HideLoading)
            }.onFailure { e ->
                _uiEvent.emit(BatchHistoryUiEvent.HideLoading)
                _uiEvent.emit(BatchHistoryUiEvent.ShowError("Failed to load batch history: ${e.message}"))
            }
        } catch (e: Exception) {
            _uiEvent.emit(BatchHistoryUiEvent.HideLoading)
            _uiEvent.emit(BatchHistoryUiEvent.ShowError("Failed to load batch history: ${e.message}"))
        }
    }

    init {
        // Initialize default dates: end date = today, start date = 1 day ago (yesterday)
        initializeDefaultDates()
        loadBatchHistory()
    }

    private fun initializeDefaultDates() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()

        // End date = current date
        val endDateStr = dateFormat.format(calendar.time)
        _endDate.value = endDateStr

        // Start date = 1 day ago
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startDateStr = dateFormat.format(calendar.time)
        _startDate.value = startDateStr
    }
}

