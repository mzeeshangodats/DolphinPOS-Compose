package com.retail.dolphinpos.presentation.features.ui.setup.cc_processing

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetHttpSettingsUseCase
import com.retail.dolphinpos.data.setup.hardware.payment.pax.GetTcpSettingsUseCase
import com.retail.dolphinpos.domain.analytics.AnalyticsTracker
import com.retail.dolphinpos.domain.model.setup.hardware.payment.pax.PaxDetail
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.GetPaxDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.InitializeTerminalUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.SavePaxDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.payment.pax.ValidateIpAddressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import javax.inject.Inject

@HiltViewModel
class CreditCardProcessingViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getTcpSettingsUseCase: GetTcpSettingsUseCase,
    private val getHttpSettingsUseCase: GetHttpSettingsUseCase,
    private val validateIpAddressUseCase: ValidateIpAddressUseCase,
    getPaxDetailsUseCase: GetPaxDetailsUseCase,
    private val savePaxDetailsUseCase: SavePaxDetailsUseCase,
    private val initializeTerminalUseCase: InitializeTerminalUseCase,
    //private val getPaxBatchInformationUseCase: GetPaxBatchInformationUseCase,
    //private val requestClosePaxBatchUseCase: RequestClosePaxBatchUseCase,
    //private val closePaxBatchUseCase: ClosePaxBatchUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _viewState = MutableStateFlow(CreditCardProcessingViewState())
    val viewState: StateFlow<CreditCardProcessingViewState> = _viewState.asStateFlow()

    private fun updateState(update: (CreditCardProcessingViewState) -> CreditCardProcessingViewState) {
        _viewState.value = update(_viewState.value)
    }

    private var paxDetails: PaxDetail? = null
    private var originalIpAddress: String = ""
    private var originalPortNumber: String = "10009"
    private var originalCommunicationType: CommunicationType = CommunicationType.TCP_IP
    private var originalTerminalType: TerminalType = TerminalType.PAX_A35
    private var originalDigitalSignatureEnabled: Boolean = true

    private fun String.toCommunicationType(): CommunicationType {
        return when (this.lowercase()) {
            "tcp/ip" -> CommunicationType.TCP_IP
            "http/get" -> CommunicationType.HTTP_GET
            else -> CommunicationType.TCP_IP // Default fallback
        }
    }

    private fun CommunicationType.toCommunicationTypeString(): String {
        return when (this) {
            CommunicationType.TCP_IP -> "tcp/ip"
            CommunicationType.HTTP_GET -> "http/get"
        }
    }

    init {
        viewModelScope.launch {
            paxDetails = getPaxDetailsUseCase()

            paxDetails?.let {
                val commType = it.communicationType.toCommunicationType()
                // Store original values for reset functionality
                originalIpAddress = it.ipAddress
                originalPortNumber = it.portNumber
                originalCommunicationType = commType
                
                val initialConfig = CreditCardConfigState(
                    ipAddress = it.ipAddress,
                    portNumber = it.portNumber,
                    communicationType = commType
                )
                
                // Store original config values
                originalTerminalType = initialConfig.selectedTerminalType
                originalDigitalSignatureEnabled = initialConfig.digitalSignatureEnabled
                
                _viewState.value = _viewState.value.copy(
                    ipAddress = it.ipAddress,
                    portNumber = it.portNumber,
                    communicationType = it.communicationType,
                    isButtonEnabled = validations(it.ipAddress, it.portNumber),
                    config = initialConfig
                )
            } ?: run {
                // If no saved data, use defaults
                originalIpAddress = ""
                originalPortNumber = "10009"
                originalCommunicationType = CommunicationType.TCP_IP
                // Store initial config defaults
                originalTerminalType = _viewState.value.config.selectedTerminalType
                originalDigitalSignatureEnabled = _viewState.value.config.digitalSignatureEnabled
            }
        }
        // Analytics: Log screen entry
        analyticsTracker.logEvent("credit_card_processing_screen_view", null)
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
        updateState {
            it.copy(
                config = it.config.copy(communicationType = communicationType),
                communicationType = communicationType.toCommunicationTypeString()
            )
        }
    }

    fun updateIpAddress(ip: String) {
        updateState {
            it.copy(
                config = it.config.copy(ipAddress = ip),
                ipAddress = ip,
                isButtonEnabled = validations(ip, it.portNumber)
            )
        }
    }

    fun updatePortNumber(port: String) {
        updateState {
            it.copy(
                config = it.config.copy(portNumber = port),
                portNumber = port,
                isButtonEnabled = validations(it.ipAddress, port)
            )
        }
    }

    fun updateTerminalId(terminalId: String) {
        updateState { it.copy(config = it.config.copy(terminalId = terminalId)) }
    }

    fun updateDigitalSignature(enabled: Boolean) {
        updateState { it.copy(config = it.config.copy(digitalSignatureEnabled = enabled)) }
    }

    fun saveConfiguration() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            try {

                savePaxDetailsUseCase(
                    ipAddress = viewState.value.ipAddress,
                    portNumber = viewState.value.portNumber,
                    communicationType = viewState.value.communicationType ?: "tcp/ip"
                )
                delay(200)

                // Analytics: Log successful update
                analyticsTracker.logEvent("pax_details_updated_successfully", Bundle().apply {
                    putString("ip_address", viewState.value.ipAddress)
                    putString("port_number", viewState.value.portNumber)
                    putString("comm_type", viewState.value.communicationType)
                })

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

    private fun initPaxCommunication(isTestConnection: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val eventNamePrefix = if (isTestConnection) "pax_test_connection" else "pax_batch_close"

            // Capture current state values before async callback
            val currentCommType = viewState.value.communicationType ?: "unknown"
            val currentIpAddress = viewState.value.ipAddress

            try {
                updateState {
                    it.copy(
                        isLoading = true,
                        errorMessage = null,
                        successMessage = null
                    )
                }

                initializeTerminalUseCase { result ->
                    if (result.isSuccess && result.session != null) {
                        // Analytics: Log successful terminal initialization
                        analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                            putString("response_type", "terminal_init_success")
                            putString("comm_type", currentCommType)
                            putString("ip_address", currentIpAddress)
                            putString("app_name", "Dolphin POS")
                        })

                        if (isTestConnection) {
                            val message = "POS Device Initialized: Dolphin POS"
                            viewModelScope.launch(Dispatchers.Main) {
                                updateState {
                                    it.copy(
                                        isLoading = false,
                                        successMessage = message
                                    )
                                }
                            }
                            // Analytics: Log successful test connection
                            analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                                putString("response_type", "success")
                                putString("app_name", "Dolphin POS")
                                putString("log_message", message)
                            })
                        }
                    } else {
                        val errorMessage = result.message ?: "Unable to connect to POS Device"
                        viewModelScope.launch(Dispatchers.Main) {
                            updateState {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = errorMessage
                                )
                            }
                        }
                        // Analytics: Log failed terminal initialization
                        analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                            putString("response_type", "terminal_init_failed")
                            putString("comm_type", currentCommType)
                            putString("ip_address", currentIpAddress)
                            putString("error_message", result.message)
                            putString("log_message", errorMessage)
                        })
                    }
                }
            } catch (e: SocketTimeoutException) {
                val errorMessage = "Error: ${e.message ?: "Socket timeout occurred"}"
                Log.e(
                    "CreditCardProcessingViewModel",
                    "initPaxCommunication: SocketTimeoutException",
                    e
                )
                viewModelScope.launch(Dispatchers.Main) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMessage
                        )
                    }
                }
                // Analytics: Log socket timeout error
                analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                    putString("response_type", "socket_timeout_error")
                    putString("error_message", e.message)
                    putString("log_message", errorMessage)
                })
            } catch (e: Exception) {
                val errorMessage =
                    "Error: ${e.message ?: "Unknown error occurred during PAX communication"}"
                Log.e("CreditCardProcessingViewModel", "initPaxCommunication: General Exception", e)
                viewModelScope.launch(Dispatchers.Main) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMessage
                        )
                    }
                }
                // Analytics: Log general error
                analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                    putString("response_type", "general_error")
                    putString("error_message", e.message)
                    putString("error_stacktrace", Log.getStackTraceString(e))
                    putString("log_message", errorMessage)
                })
            }
        }
    }

    fun testConnection() {
        initPaxCommunication(isTestConnection = true)
    }

    fun onCancel() {
        // Reset to original saved values
        updateState {
            it.copy(
                ipAddress = originalIpAddress,
                portNumber = originalPortNumber,
                communicationType = originalCommunicationType.toCommunicationTypeString(),
                isButtonEnabled = validations(originalIpAddress, originalPortNumber),
                config = CreditCardConfigState(
                    selectedTerminalType = originalTerminalType,
                    communicationType = originalCommunicationType,
                    ipAddress = originalIpAddress,
                    portNumber = originalPortNumber,
                    digitalSignatureEnabled = originalDigitalSignatureEnabled
                ),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun validations(ipAddress: String, portNumber: String): Boolean {
        return ipAddress.isNotEmpty() && portNumber.isNotEmpty() && validateIpAddressUseCase(
            ipAddress
        )
    }

}
