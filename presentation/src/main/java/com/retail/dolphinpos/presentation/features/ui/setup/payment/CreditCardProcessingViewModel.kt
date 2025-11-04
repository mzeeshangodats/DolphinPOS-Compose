package com.retail.dolphinpos.presentation.features.ui.setup.payment

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

    init {
        viewModelScope.launch {
            paxDetails = getPaxDetailsUseCase()

            paxDetails?.let {
                _viewState.value = _viewState.value.copy(
                    ipAddress = it.ipAddress,
                    portNumber = it.portNumber,
                    communicationType = it.communicationType,
                    isButtonEnabled = validations(it.ipAddress, it.portNumber)
                )
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
        updateState { it.copy(config = it.config.copy(communicationType = communicationType)) }
    }

    fun updateIpAddress(ip: String) {
        updateState {
            it.copy(
                config = it.config.copy(ipAddress = ip),
                isButtonEnabled = validations(ip, it.portNumber)
            )
        }
    }

    fun updatePortNumber(port: String) {
        updateState {
            it.copy(
                config = it.config.copy(portNumber = port),
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
            val commType = viewState.value.config.communicationType
            val eventNamePrefix = if (isTestConnection) "pax_test_connection" else "pax_batch_close"

            val settings =
                if (commType == CommunicationType.HTTP_GET) getHttpSettingsUseCase(
                    viewState.value.ipAddress,
                    viewState.value.portNumber,
                    isTestConnection = true
                ) else getTcpSettingsUseCase(
                    viewState.value.ipAddress,
                    viewState.value.portNumber,
                    isTestConnection = true
                )

            /*try {
                val settings =
                    if (commType == "http/get") getHttpSettingsUseCase(
                        viewState.value.ipAddress,
                        viewState.value.portNumber,
                        isTestConnection = true
                    ) else getTcpSettingsUseCase(
                        viewState.value.ipAddress,
                        viewState.value.portNumber,
                        isTestConnection = true
                    )

                val terminal = POSLinkSemi.getInstance().getTerminal(
                    context,
                    settings
                )

                if (terminal != null) {
                    val result = terminal.manage.init()
                    if (result.isSuccessful) {
                        // Analytics: Log successful terminal initialization
                        analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                            putString("response_type", "terminal_init_success")
                            putString("comm_type", commType)
                            putString("ip_address", viewState.value.ipAddress)
                        })

                        if (isTestConnection) {
                            val response = result.response()
                            getPaxBatchInformationUseCase(
                                terminal,
                                onSuccess = { variableResponse ->
                                    message = "POS Device Initialized: ${response.appName()}\nBatch no: " + variableResponse.variableValue1()
                                    emitViewEffect(ShowInformationSnackBar(message))
                                    // Analytics: Log successful test connection with batch info
                                    analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                                        putString("response_type", "success_with_batch_info")
                                        putString("app_name", response.appName())
                                        putString("batch_number", variableResponse.variableValue1())
                                        putString("log_message", message)
                                    })
                                },
                                onFailure = {
                                    message = "POS Device Initialized ${response.appName()}: Unable to get Pax batch information"
                                    emitViewEffect(ShowErrorSnackBar(message))
                                    // Analytics: Log successful test connection but failed to get batch info
                                    analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                                        putString("response_type", "success_no_batch_info")
                                        putString("app_name", response.appName())
                                        putString("log_message", message)
                                    })
                                })
                        } else {
                            setIsLoading(message = "Do not close The App or Pax batch close in progress...",isLoading = true)
                            requestClosePaxBatchUseCase(
                                terminal,
                                onSuccess = { response ->
                                    message = "Batch Closed Successfully (${response.hostInformation().batchNumber()})"
                                    emitViewEffect(ShowSuccessSnackBar(message)) // Show success snack bar first
                                    // Analytics: Log successful PAX batch close
                                    analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                                        putString("response_type", "pax_close_success")
                                        putString("batch_number", response.hostInformation().batchNumber())
                                        putString("log_message", message)
                                    })
                                    handleBatchCloseSuccessfully(response)
                                },
                                onFailure = { response ->
                                    message = "Failed to Close Batch : $response"
                                    emitViewEffect(ShowErrorSnackBar(message))
                                    // Analytics: Log failed PAX batch close
                                    analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                                        putString("response_type", "pax_close_failed")
                                        putString("error_response", response)
                                        putString("log_message", message)
                                    })
                                })
                        }
                    } else {
                        message = "Unable to connect to POS Device, please try again."
                        emitViewEffect(ShowErrorSnackBar(message))
                        // Analytics: Log failed terminal initialization
                        analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                            putString("response_type", "terminal_init_failed")
                            putString("comm_type", commType)
                            putString("ip_address", viewState.value.ipAddress)
                            putString("error_message", result.message())
                            putString("log_message", message)
                        })
                    }
                } else {
                    message = "Failed to initialize terminal instance. Connection Timeout"
                    emitViewEffect(ShowErrorSnackBar(message))
                    // Analytics: Log terminal instance null
                    analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                        putString("comm_type", commType)
                        putString("ip_address", viewState.value.ipAddress)
                        putString("response_type", "terminal_instance_null")
                        putString("log_message", message)
                    })
                }
            } catch (e: SocketTimeoutException) {
                message = "Error: ${e.message ?: "Socket timeout occurred"}"
                Log.e(TAG, "initPaxCommunication: SocketTimeoutException", e)
                emitViewEffect(ShowErrorSnackBar(message))
                // Analytics: Log socket timeout error
                analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                    putString("response_type", "socket_timeout_error")
                    putString("error_message", e.message)
                    putString("log_message", message)
                })
            } catch (e: Exception) {
                message = "Error: ${e.message ?: "Unknown error occurred during PAX communication"}"
                Log.e(TAG, "initPaxCommunication: General Exception", e)
                emitViewEffect(ShowErrorSnackBar(message))
                // Analytics: Log general error
                analyticsTracker.logEvent(eventNamePrefix, Bundle().apply {
                    putString("response_type", "general_error")
                    putString("error_message", e.message)
                    putString("error_stacktrace", Log.getStackTraceString(e))
                    putString("log_message", message)
                })
            } finally {
                // Removed redundant snackbar here as it's handled in specific success/failure branches
                setIsLoading(message = "", isLoading = false)
            }*/
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            try {

                initPaxCommunication(isTestConnection = true)

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

    private fun validations(ipAddress: String, portNumber: String): Boolean {
        return ipAddress.isNotEmpty() && portNumber.isNotEmpty() && validateIpAddressUseCase(
            ipAddress
        )
    }

}
