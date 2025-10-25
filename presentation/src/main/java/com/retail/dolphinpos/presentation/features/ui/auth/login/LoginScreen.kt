package com.retail.dolphinpos.presentation.features.ui.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarAuth
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.Loader
import com.retail.dolphinpos.presentation.util.DialogHandler

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isButtonEnabled = username.isNotBlank() && password.isNotBlank()

    val pleaseWait = stringResource(id = R.string.plz_wait)
    val tryAgain = stringResource(id = R.string.try_again)

    LaunchedEffect(viewModel.loginUiEvent) {
        when (val event = viewModel.loginUiEvent) {
            is LoginUiEvent.ShowLoading -> Loader.show(pleaseWait)
            is LoginUiEvent.HideLoading -> Loader.hide()
            is LoginUiEvent.ShowError -> {
                DialogHandler.showDialog(message = event.message, buttonText = tryAgain) {}
            }

            is LoginUiEvent.NavigateToRegister -> {
                navController.navigate("selectRegister")
            }
            null -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.splash_background_image),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        HeaderAppBarAuth()

        // Foreground layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side - Texts
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BaseText(
                    text = stringResource(id = R.string.let_s_get_started),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 34F
                )

                Spacer(modifier = Modifier.height(10.dp))

                BaseText(
                    text = stringResource(id = R.string.log_in_to_your_retail_pos_system_and_manage_transactions_effortlessly),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18F,
                    textAlign = TextAlign.Center
                )
            }

            // Right Side - Card
            Card(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .padding(horizontal = 40.dp, vertical = 60.dp),
                shape = RoundedCornerShape(5.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.bgColorPrimary)
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // 🔹 Logo at Top Center
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(120.dp)
                                .padding(top = 30.dp, bottom = 20.dp)
                        )
                    }

                    // 🔹 Fields in Center
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Username
                        BaseText(
                            text = "Username",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorResource(id = R.color.bgColorLabels),
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.SemiBold
                        )
                        BaseOutlinedEditText(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = stringResource(id = R.string.enter_username)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password
                        BaseText(
                            text = stringResource(id = R.string.password),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorResource(id = R.color.bgColorLabels),
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.SemiBold
                        )

                        BaseOutlinedEditText(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = stringResource(id = R.string.enter_password)
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // Login Button
                        BaseButton(
                            text = stringResource(id = R.string.login),
                            enabled = isButtonEnabled,
                            onClick = {
                                if (isButtonEnabled) {
                                    viewModel.login("imran_123", "1234")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

