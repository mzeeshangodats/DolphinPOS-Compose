package com.retail.dolphinpos.presentation.features.ui.reports.transaction_activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.dao.TransactionDao
import com.retail.dolphinpos.data.entities.transaction.PaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionActivityItemData(
    val id: Long,
    val orderNo: String?,
    val invoiceNo: String?,
    val paymentMethod: PaymentMethod,
    val status: String,
    val amount: Double,
    val tax: Double?,
    val createdAt: Long
)

sealed class TransactionActivityUiEvent {
    object ShowLoading : TransactionActivityUiEvent()
    object HideLoading : TransactionActivityUiEvent()
    data class ShowError(val message: String) : TransactionActivityUiEvent()
}

@HiltViewModel
class TransactionActivityViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<TransactionActivityItemData>>(emptyList())
    val transactions: StateFlow<List<TransactionActivityItemData>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<TransactionActivityUiEvent>()
    val uiEvent: SharedFlow<TransactionActivityUiEvent> = _uiEvent.asSharedFlow()

    fun loadTransactions() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiEvent.emit(TransactionActivityUiEvent.ShowLoading)

                val storeId = preferenceManager.getStoreID()
                val transactionEntities = transactionDao.getTransactionsByStoreId(storeId)

                val transactionList = transactionEntities.map { entity ->
                    TransactionActivityItemData(
                        id = entity.id,
                        orderNo = entity.orderNo,
                        invoiceNo = entity.invoiceNo,
                        paymentMethod = entity.paymentMethod,
                        status = entity.status,
                        amount = entity.amount,
                        tax = entity.tax,
                        createdAt = entity.createdAt
                    )
                }

                _transactions.value = transactionList

                _isLoading.value = false
                _uiEvent.emit(TransactionActivityUiEvent.HideLoading)
            } catch (e: Exception) {
                _isLoading.value = false
                _uiEvent.emit(TransactionActivityUiEvent.HideLoading)
                _uiEvent.emit(TransactionActivityUiEvent.ShowError(e.message ?: "Failed to load transactions"))
            }
        }
    }
}

