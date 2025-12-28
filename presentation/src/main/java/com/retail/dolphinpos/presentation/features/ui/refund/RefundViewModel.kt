package com.retail.dolphinpos.presentation.features.ui.refund

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.domain.model.refund.Refund
import com.retail.dolphinpos.domain.model.refund.RefundRequest
import com.retail.dolphinpos.domain.model.refund.RefundType
import com.retail.dolphinpos.domain.usecases.refund.RefundUseCase
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
class RefundViewModel @Inject constructor(
    private val refundUseCase: RefundUseCase,
    private val application: Application
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RefundUiState())
    val uiState: StateFlow<RefundUiState> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<RefundEvent>()
    val uiEvent: SharedFlow<RefundEvent> = _uiEvent.asSharedFlow()
    
    fun processRefund(request: RefundRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            refundUseCase.executeRefund(request, application).fold(
                onSuccess = { refund ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        refund = refund
                    )
                    _uiEvent.emit(RefundEvent.RefundSuccess(refund))
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Refund failed"
                    )
                    _uiEvent.emit(RefundEvent.RefundError(error.message ?: "Refund failed"))
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun resetState() {
        _uiState.value = RefundUiState()
    }
}

