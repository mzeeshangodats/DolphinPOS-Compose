package com.retail.dolphinpos.presentation.features.ui.setup.cfd

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.retail.dolphinpos.common.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CustomerDisplaySetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val gson: Gson
) : ViewModel() {

    private val _viewState = MutableStateFlow(CustomerDisplaySetupViewState())
    val viewState: StateFlow<CustomerDisplaySetupViewState> = _viewState.asStateFlow()

    private var originalIpAddress: String = ""
    private var originalIsEnabled: Boolean = false

    init {
        loadSavedConfiguration()
    }

    private fun loadSavedConfiguration() {
        viewModelScope.launch {
            originalIpAddress = preferenceManager.getCustomerDisplayIpAddress()
            originalIsEnabled = preferenceManager.isCustomerDisplayEnabled()

            _viewState.value = _viewState.value.copy(
                ipAddress = originalIpAddress,
                isEnabled = originalIsEnabled,
                isButtonEnabled = if (originalIsEnabled) isValidIpAddress(originalIpAddress) else true
            )
        }
    }

    private fun updateState(update: (CustomerDisplaySetupViewState) -> CustomerDisplaySetupViewState) {
        _viewState.value = update(_viewState.value)
    }

    fun updateIpAddress(ip: String) {
        updateState {
            it.copy(
                ipAddress = ip,
                isButtonEnabled = if (it.isEnabled) isValidIpAddress(ip) else true
            )
        }
    }

    fun updateEnabled(enabled: Boolean) {
        updateState {
            it.copy(
                isEnabled = enabled,
                isButtonEnabled = if (enabled) isValidIpAddress(it.ipAddress) else true
            )
        }
    }

    fun saveConfiguration() {
        viewModelScope.launch {
            // Validate IP address if enabled
            if (_viewState.value.isEnabled && _viewState.value.ipAddress.isBlank()) {
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = "IP Address is required when Customer Display is enabled",
                        successMessage = null
                    )
                }
                return@launch
            }

            // Validate IP address format if provided
            if (_viewState.value.ipAddress.isNotEmpty() && !isValidIpAddressFormat(_viewState.value.ipAddress)) {
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = "IP Address must be in format xxx.xxx.xxx.xxx",
                        successMessage = null
                    )
                }
                return@launch
            }

            updateState { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            try {
                // Save to preferences
                preferenceManager.setCustomerDisplayIpAddress(_viewState.value.ipAddress)
                preferenceManager.setCustomerDisplayEnabled(_viewState.value.isEnabled)

                // Update original values
                originalIpAddress = _viewState.value.ipAddress
                originalIsEnabled = _viewState.value.isEnabled

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

    fun onCancel() {
        // Reset to original saved values
        updateState {
            it.copy(
                ipAddress = originalIpAddress,
                isEnabled = originalIsEnabled,
                isButtonEnabled = isValidIpAddress(originalIpAddress),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun clearMessages() {
        updateState { it.copy(errorMessage = null, successMessage = null) }
    }

    fun clearNavigation() {
        updateState { it.copy(shouldNavigateBack = false) }
    }

    private fun isValidIpAddress(ipAddress: String): Boolean {
        return ipAddress.isNotEmpty() && isValidIpAddressFormat(ipAddress)
    }

    /**
     * Validates IP address format (xxx.xxx.xxx.xxx)
     */
    private fun isValidIpAddressFormat(ipAddress: String): Boolean {
        val ipPattern = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
        return ipPattern.matches(ipAddress.trim())
    }

    fun testConnection() {
        viewModelScope.launch {
            // Validate IP address
            if (_viewState.value.ipAddress.isBlank()) {
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = "IP Address is required",
                        successMessage = null
                    )
                }
                return@launch
            }

            // Validate IP address format
            if (!isValidIpAddressFormat(_viewState.value.ipAddress)) {
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = "IP Address must be in format xxx.xxx.xxx.xxx",
                        successMessage = null
                    )
                }
                return@launch
            }

            updateState { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            try {
                val testClient = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("ws://${_viewState.value.ipAddress}:8080/customer-display")
                    .build()

                var connectionSuccess = false
                var connectionError: String? = null

                val webSocket = testClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        connectionSuccess = true
                        // Close immediately after successful connection
                        webSocket.close(1000, "Test connection successful")
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        connectionError = t.message ?: "Connection failed"
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        // Connection closed
                    }
                })

                // Wait for connection result (max 5 seconds)
                var attempts = 0
                while (attempts < 50 && !connectionSuccess && connectionError == null) {
                    delay(100)
                    attempts++
                }

                // Close the test connection
                webSocket.close(1000, "Test complete")

                if (connectionSuccess) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            successMessage = "Connection successful! WebSocket server is reachable at ${_viewState.value.ipAddress}:8080"
                        )
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = connectionError ?: "Connection failed: Unable to reach WebSocket server at ${_viewState.value.ipAddress}:8080. Make sure Customer Display is enabled on the POS device."
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Connection test failed: ${e.message ?: "Unknown error"}"
                    )
                }
            }
        }
    }
}

