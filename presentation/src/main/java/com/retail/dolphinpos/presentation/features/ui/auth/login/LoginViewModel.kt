package com.retail.dolphinpos.presentation.features.ui.auth.login

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.auth.login.request.LoginRequest
import com.retail.dolphinpos.domain.repositories.auth.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class LoginViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val repository: LoginRepository,
    private val preferenceManager: PreferenceManager
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
            try {
                val response = repository.login(LoginRequest(username, password))
                isLoading = false
                loginUiEvent = LoginUiEvent.HideLoading

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
