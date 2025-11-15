package com.retail.dolphinpos.presentation.features.ui.customer_display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.customer_display.CustomerDisplayClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerDisplayViewModel @Inject constructor(
    private val gson: Gson,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private var client: CustomerDisplayClient? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _cartData = MutableStateFlow<com.retail.dolphinpos.data.customer_display.CartDisplayData?>(null)
    val cartData: StateFlow<com.retail.dolphinpos.data.customer_display.CartDisplayData?> = _cartData.asStateFlow()

    fun connect() {
        val serverIp = preferenceManager.getCustomerDisplayIpAddress()
        if (serverIp.isEmpty()) {
            _connectionState.value = ConnectionState.Disconnected
            return
        }

        viewModelScope.launch {
            try {
                client = CustomerDisplayClient(gson, serverIp, 8080)
                
                // Observe connection state
                launch {
                    client?.connectionState?.collect { state ->
                        _connectionState.value = when (state) {
                            is CustomerDisplayClient.ConnectionState.Disconnected -> ConnectionState.Disconnected
                            is CustomerDisplayClient.ConnectionState.Connecting -> ConnectionState.Connecting
                            is CustomerDisplayClient.ConnectionState.Connected -> ConnectionState.Connected
                        }
                    }
                }
                
                // Observe cart data
                launch {
                    client?.cartData?.collect { data ->
                        _cartData.value = data
                    }
                }
                
                client?.connect()
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    fun disconnect() {
        client?.disconnect()
        client = null
        _connectionState.value = ConnectionState.Disconnected
        _cartData.value = null
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
    }
}

