package com.retail.dolphinpos.presentation.features.ui.auth.login

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.domain.model.auth.login.request.LoginRequest
import com.retail.dolphinpos.domain.repositories.auth.LoginRepository
import com.retail.dolphinpos.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class LoginViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val repository: LoginRepository,
    private val preferenceManager: PreferenceManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    // 🔹 Compose state
    var loginUiEvent by mutableStateOf<LoginUiEvent?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun login(username: String, password: String) {
        viewModelScope.launch {
            isLoading = true
            loginUiEvent = LoginUiEvent.ShowLoading
            
            // Check internet connection before attempting login
            if (!networkMonitor.isNetworkAvailable()) {
                isLoading = false
                loginUiEvent = LoginUiEvent.HideLoading
                loginUiEvent = LoginUiEvent.ShowNoInternetDialog(context.getString(R.string.no_internet_connection))
                return@launch
            }
            
            try {
                val response = repository.login(LoginRequest(username, password))
                isLoading = false
                loginUiEvent = LoginUiEvent.HideLoading

                // Check if there are validation errors
                response.errors?.let { errors ->
                    val errorMessages = buildString {
                        errors.getUsernameError()?.let { append("Username: $it\n") }
                        errors.getPasswordError()?.let { append("Password: $it\n") }
                    }.trim().ifEmpty { "Please check your credentials" }
                    loginUiEvent = LoginUiEvent.ShowError(errorMessages)
                    return@launch
                }

                response.loginData?.let { loginData ->
                    preferenceManager.saveLoginData(loginData, password)
                    loginData.storeInfo.logoUrl?.let {
                        loginData.storeInfo.locations?.let { locationsList ->
                            repository.insertUsersDataIntoLocalDB(
                                loginData.allStoreUsers,
                                loginData.storeInfo,
                                it,
                                locationsList,
                                preferenceManager.getPassword(),
                                loginData.user.id,
                                loginData.user.storeId,
                                loginData.user.locationId
                            )
                        }
                    }
                    loginUiEvent = LoginUiEvent.NavigateToRegister

                } ?: run {
                    loginUiEvent =
                        LoginUiEvent.ShowError(response.message ?: "No data received")
                }
            } catch (e: Exception) {
                isLoading = false
                loginUiEvent = LoginUiEvent.HideLoading
                loginUiEvent =
                    LoginUiEvent.ShowError(e.message ?: "Something went wrong")
            }
        }
    }
}
