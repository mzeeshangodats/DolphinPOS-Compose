package com.retail.dolphinpos.presentation.features.ui.reports.batch_history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.data.dao.UserDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchHistoryItemData(
    val batchId: Int,
    val batchNo: String,
    val startingCashAmount: Double,
    val closingCashAmount: Double?,
    val startedAt: Long,
    val closedAt: Long?,
    val isClosed: Boolean
)

sealed class BatchHistoryUiEvent {
    object ShowLoading : BatchHistoryUiEvent()
    object HideLoading : BatchHistoryUiEvent()
    data class ShowError(val message: String) : BatchHistoryUiEvent()
}

@HiltViewModel
class BatchHistoryViewModel @Inject constructor(
    private val userDao: UserDao
) : ViewModel() {

    private val _batches = MutableStateFlow<List<BatchHistoryItemData>>(emptyList())
    val batches: StateFlow<List<BatchHistoryItemData>> = _batches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<BatchHistoryUiEvent>()
    val uiEvent: SharedFlow<BatchHistoryUiEvent> = _uiEvent.asSharedFlow()

    fun loadBatchHistory() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiEvent.emit(BatchHistoryUiEvent.ShowLoading)

                val batchEntities = userDao.getAllBatches()
                val batchList = batchEntities.map { entity ->
                    BatchHistoryItemData(
                        batchId = entity.batchId,
                        batchNo = entity.batchNo,
                        startingCashAmount = entity.startingCashAmount,
                        closingCashAmount = entity.closingCashAmount,
                        startedAt = entity.startedAt,
                        closedAt = entity.closedAt,
                        isClosed = entity.closedAt != null
                    )
                }

                _batches.value = batchList

                _isLoading.value = false
                _uiEvent.emit(BatchHistoryUiEvent.HideLoading)
            } catch (e: Exception) {
                _isLoading.value = false
                _uiEvent.emit(BatchHistoryUiEvent.HideLoading)
                _uiEvent.emit(BatchHistoryUiEvent.ShowError(e.message ?: "Failed to load batch history"))
            }
        }
    }
}

