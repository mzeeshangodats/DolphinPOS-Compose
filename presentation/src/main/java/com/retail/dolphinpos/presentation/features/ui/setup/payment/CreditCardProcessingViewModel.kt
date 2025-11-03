package com.retail.dolphinpos.presentation.features.ui.setup.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreditCardProcessingViewModel @Inject constructor() : ViewModel() {

    private val _viewState = MutableStateFlow(CreditCardProcessingViewState())
    val viewState: StateFlow<CreditCardProcessingViewState> = _viewState.asStateFlow()

    private fun updateState(update: (CreditCardProcessingViewState) -> CreditCardProcessingViewState) {
        _viewState.value = update(_viewState.value)
    }

    fun clearMessages() {
        updateState { it.copy(errorMessage = null, successMessage = null) }
    }

    fun clearNavigation() {
        updateState { it.copy(shouldNavigateBack = false) }
    }

    fun updateIsEnabled(enabled: Boolean) {
        updateState { it.copy(config = it.config.copy(isEnabled = enabled)) }
    }

    fun updateTerminalType(terminalType: TerminalType) {
        updateState { currentState ->
            val updatedConfig = if (terminalType == TerminalType.PAX_A35) {
                currentState.config.copy(
                    selectedTerminalType = terminalType,
                    portNumber = "10009"
                )
            } else {
                currentState.config.copy(selectedTerminalType = terminalType)
            }
            currentState.copy(config = updatedConfig)
        }
    }

    fun updateCommunicationType(communicationType: CommunicationType) {
        updateState { it.copy(config = it.config.copy(communicationType = communicationType)) }
    }

    fun updateBluetoothAddress(address: String) {
        updateState { it.copy(config = it.config.copy(bluetoothAddress = address)) }
    }

    fun updateIpAddress(ip: String) {
        updateState { it.copy(config = it.config.copy(ipAddress = ip)) }
    }

    fun updatePortNumber(port: String) {
        updateState { it.copy(config = it.config.copy(portNumber = port)) }
    }

    fun updateTerminalId(terminalId: String) {
        updateState { it.copy(config = it.config.copy(terminalId = terminalId)) }
    }

    fun updateMerchantId(merchantId: String) {
        updateState { it.copy(config = it.config.copy(merchantId = merchantId)) }
    }

    fun updateDigitalSignature(enabled: Boolean) {
        updateState { it.copy(config = it.config.copy(digitalSignatureEnabled = enabled)) }
    }

    fun saveConfiguration() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            
            try {
                // TODO: Implement save logic
                // For now, just simulate success
                kotlinx.coroutines.delay(1000)
                
                updateState { 
                    it.copy(
                        isLoading = false, 
                        successMessage = "Configuration saved successfully"
                    ) 
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = e.message ?: "Failed to save configuration"
                    ) 
                }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            
            try {
                // TODO: Implement connection test logic
                kotlinx.coroutines.delay(2000)
                
                updateState { 
                    it.copy(
                        isLoading = false, 
                        successMessage = "Connection test successful"
                    ) 
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = e.message ?: "Connection test failed"
                    ) 
                }
            }
        }
    }

    fun onCancel() {
        updateState { it.copy(shouldNavigateBack = true) }
    }
}
