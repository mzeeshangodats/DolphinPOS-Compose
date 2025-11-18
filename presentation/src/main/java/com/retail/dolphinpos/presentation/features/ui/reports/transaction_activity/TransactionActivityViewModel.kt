package com.retail.dolphinpos.presentation.features.ui.reports.transaction_activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.entities.transaction.PaymentMethod
import com.retail.dolphinpos.domain.model.transaction.Transaction
import com.retail.dolphinpos.domain.repositories.transaction.TransactionRepository
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
    val createdAt: Long,
    val taxDetails: List<com.retail.dolphinpos.domain.model.TaxDetail>? = null,  // Tax breakdown
    val taxExempt: Boolean = false  // Tax exempt status
)

sealed class TransactionActivityUiEvent {
    object ShowLoading : TransactionActivityUiEvent()
    object HideLoading : TransactionActivityUiEvent()
    data class ShowError(val message: String) : TransactionActivityUiEvent()
}

@HiltViewModel
class TransactionActivityViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferenceManager: PreferenceManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<TransactionActivityItemData>>(emptyList())
    val transactions: StateFlow<List<TransactionActivityItemData>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()

    private val _uiEvent = MutableSharedFlow<TransactionActivityUiEvent>()
    val uiEvent: SharedFlow<TransactionActivityUiEvent> = _uiEvent.asSharedFlow()

    private var currentPage = 1
    private val pageLimit = 10

    fun loadTransactions(reset: Boolean = true) {
        viewModelScope.launch {
            try {
                if (reset) {
                    _isLoading.value = true
                    _uiEvent.emit(TransactionActivityUiEvent.ShowLoading)
                    currentPage = 1
                    _hasMorePages.value = true
                } else {
                    _isLoadingMore.value = true
                }

                val storeId = preferenceManager.getStoreID()
                val locationId = preferenceManager.getOccupiedLocationID()

                val transactionList: List<Transaction> = if (networkMonitor.isNetworkAvailable()) {
                    // If internet is available, fetch from API and sync
                    try {
                        // Fetch from API and save to local DB
                        val apiTransactions = transactionRepository.fetchAndSaveTransactionsFromApi(
                            storeId = storeId,
                            locationId = if (locationId > 0) locationId else null,
                            page = currentPage,
                            limit = pageLimit
                        )

                        // Check if there are more pages
                        val response = transactionRepository.getTransactionsFromApi(
                            storeId = storeId,
                            locationId = if (locationId > 0) locationId else null,
                            page = currentPage,
                            limit = pageLimit
                        )
                        val totalRecords = response.data?.totalRecords ?: 0
                        val currentRecords = if (reset) {
                            apiTransactions.size
                        } else {
                            _transactions.value.size + apiTransactions.size
                        }
                        _hasMorePages.value = currentRecords < totalRecords

                        apiTransactions
                    } catch (e: Exception) {
                        // If API call fails, fallback to local DB
                        android.util.Log.e("TransactionActivity", "API call failed: ${e.message}")
                        if (reset) {
                            transactionRepository.getTransactionsFromLocal(storeId)
                        } else {
                            emptyList()
                        }
                    }
                } else {
                    // No internet - fetch from local DB
                    if (reset) {
                        transactionRepository.getTransactionsFromLocal(storeId)
                    } else {
                        emptyList() // Can't load more from local DB without pagination
                    }
                }

                // Sync unsynced transactions if internet is available (only on initial load)
                if (reset && networkMonitor.isNetworkAvailable()) {
                    syncUnsyncedTransactions()
                }

                val transactionItemList = transactionList.map { transaction ->
                    // Calculate taxExempt: true if tax is null/0 and taxDetails is empty/null
                    val isTaxExempt = (transaction.tax == null || transaction.tax == 0.0) &&
                            (transaction.taxDetails == null
                                    || (transaction.taxDetails?.isEmpty() ?: false))

                    TransactionActivityItemData(
                        id = transaction.id,
                        orderNo = transaction.orderNo,
                        invoiceNo = transaction.invoiceNo,
                        paymentMethod = PaymentMethod.fromString(transaction.paymentMethod),
                        status = transaction.status,
                        amount = transaction.amount,
                        tax = transaction.tax,
                        createdAt = transaction.createdAt,
                        taxDetails = transaction.taxDetails,
                        taxExempt = isTaxExempt
                    )
                }

                if (reset) {
                    _transactions.value = transactionItemList
                } else {
                    _transactions.value = _transactions.value + transactionItemList
                }

                _isLoading.value = false
                _isLoadingMore.value = false
                _uiEvent.emit(TransactionActivityUiEvent.HideLoading)
            } catch (e: Exception) {
                _isLoading.value = false
                _isLoadingMore.value = false
                _uiEvent.emit(TransactionActivityUiEvent.HideLoading)
                _uiEvent.emit(
                    TransactionActivityUiEvent.ShowError(
                        e.message ?: "Failed to load transactions"
                    )
                )
            }
        }
    }

    fun loadMoreTransactions() {
        if (!_isLoadingMore.value && _hasMorePages.value && networkMonitor.isNetworkAvailable()) {
            currentPage++
            loadTransactions(reset = false)
        }
    }

    private suspend fun syncUnsyncedTransactions() {
        try {
            val unsyncedTransactions = transactionRepository.getUnsyncedTransactions()
            unsyncedTransactions.forEach { transaction ->
                val result = transactionRepository.syncTransactionToServer(transaction)
                result.onSuccess {
                    // Update transaction status to "paid" after successful sync
                    val updatedTransaction = transaction.copy(
                        status = "paid",
                        updatedAt = System.currentTimeMillis()
                    )
                    transactionRepository.updateTransaction(updatedTransaction)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionActivity", "Failed to sync transactions: ${e.message}")
        }
    }
}
