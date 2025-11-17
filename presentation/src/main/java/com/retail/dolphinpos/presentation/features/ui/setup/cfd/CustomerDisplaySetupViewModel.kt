package com.retail.dolphinpos.presentation.features.ui.setup.cfd

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.customer_display.CustomerDisplayManager
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
    private val gson: Gson,
    private val customerDisplayManager: CustomerDisplayManager
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

                // Connect to CFD device based on enabled status
                android.util.Log.d("CustomerDisplaySetup", "Connecting to CFD device after configuration save...")
                customerDisplayManager.restartConnectionIfNeeded()
                
                val cfdIp = customerDisplayManager.getCfdIpAddress()
                android.util.Log.d("CustomerDisplaySetup", "CFD IP address: $cfdIp")

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

    fun testConnection() {
        viewModelScope.launch {
            android.util.Log.d("CustomerDisplaySetup", "=== Testing POS Server Status ===")
            updateState { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            try {
                // First, ensure server is running
                if (!_viewState.value.isEnabled) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Please enable Customer Display first, then save configuration"
                        )
                    }
                    return@launch
                }

                // Get CFD IP address
                val cfdIp = _viewState.value.ipAddress
                if (cfdIp.isBlank()) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Please enter the CFD device IP address first"
                        )
                    }
                    return@launch
                }

                android.util.Log.d("CustomerDisplaySetup", "Testing connection to CFD device: $cfdIp")
                
                // Connect to CFD device
                customerDisplayManager.restartConnectionIfNeeded()
                
                // Wait a bit for connection
                delay(1000)
                
                val isConnected = customerDisplayManager.isConnected()
                android.util.Log.d("CustomerDisplaySetup", "Connected to CFD: $isConnected")

                // Test connection by trying to connect to CFD device
                val port = 8080
                val url = "ws://$cfdIp:$port/customer-display"
                
                android.util.Log.d("CustomerDisplaySetup", "Testing CFD device connectivity: $url")
                
                val testClient = OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(3, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .build()

                var connectionSuccess = false
                var connectionError: String? = null

                val webSocket = testClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        android.util.Log.d("CustomerDisplaySetup", "✓ Server is accessible!")
                        connectionSuccess = true
                        webSocket.close(1000, "Test successful")
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        android.util.Log.e("CustomerDisplaySetup", "✗ Server test failed", t)
                        connectionError = t.message ?: "Connection failed"
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        android.util.Log.d("CustomerDisplaySetup", "Test connection closed")
                    }
                })

                // Wait for result (max 3 seconds)
                var attempts = 0
                while (attempts < 30 && !connectionSuccess && connectionError == null) {
                    delay(100)
                    attempts++
                }

                webSocket.close(1000, "Test complete")

                if (connectionSuccess) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            successMessage = "✓ Connection successful!\n\nConnected to CFD device at $cfdIp:8080\n\nThe POS will now send cart updates to this device."
                        )
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Connection failed: ${connectionError ?: "Unable to connect to CFD device"}\n\nCFD Device IP: $cfdIp:8080\n\nMake sure:\n1. CFD device is running and listening on port 8080\n2. Both devices are on the same Wi-Fi network\n3. Firewall is not blocking the connection"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomerDisplaySetup", "✗ Exception during server test", e)
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Test failed: ${e.message ?: "Unknown error"}"
                    )
                }
            }
        }
    }
}

