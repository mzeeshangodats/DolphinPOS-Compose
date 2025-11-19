package com.retail.dolphinpos.presentation.features.ui.auth.select_register

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collectLatest
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
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.DropdownSelector
import com.retail.dolphinpos.common.components.HeaderAppBarAuth
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.Loader
import com.retail.dolphinpos.presentation.util.DialogHandler

@Composable
fun SelectRegisterScreen(
    navController: NavController, viewModel: SelectRegisterViewModel = hiltViewModel()
) {
    var locations by remember { mutableStateOf(listOf<Locations>()) }
    var registers by remember { mutableStateOf(listOf<Registers>()) }

    var selectedLocation by remember { mutableStateOf<Locations?>(null) }
    var selectedRegister by remember { mutableStateOf<Registers?>(null) }

    // Get the hint text for comparison
    val selectRegisterHint = stringResource(R.string.select_register_hint)
    
    val selectRegisterErrorMessage = stringResource(R.string.select_register_error_message)
    
    // Derived state for button enabled - check if register is actually selected (not showing hint)
    val isButtonEnabled = remember(selectedLocation, selectedRegister, registers, selectRegisterHint) {
        selectedLocation != null && 
        selectedRegister != null &&
        registers.isNotEmpty()
    }

    val locationError = stringResource(id = R.string.no_loc_found)
    val registerError = stringResource(id = R.string.no_register_found)

    val pleaseWait = stringResource(id = R.string.plz_wait)
    val tryAgain = stringResource(id = R.string.try_again)

    // 🔹 Handle events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.selectRegisterUiEvent.collectLatest { event ->
            when (event) {
                is SelectRegisterUiEvent.ShowLoading -> {
                    Loader.show(pleaseWait)
                }

                is SelectRegisterUiEvent.HideLoading -> {
                    Loader.hide()
                }

                is SelectRegisterUiEvent.ShowError -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = tryAgain
                    ) {}
                }

                is SelectRegisterUiEvent.NavigateToLoginScreen -> {
                    navController.navigate("login")
                }

                is SelectRegisterUiEvent.NavigateToPinScreen -> {
                    navController.navigate("pinCode")
                }

                is SelectRegisterUiEvent.PopulateLocationsList -> {
                    locations = event.locationsList
                    if (event.locationsList.isEmpty()) {
                        DialogHandler.showDialog(
                            message = locationError,
                            buttonText = "OK",
                            iconRes = R.drawable.info_icon
                        ) {}
                    } else {
                        selectedLocation = event.locationsList[0]
                    }
                }

                is SelectRegisterUiEvent.PopulateRegistersList -> {
                    registers = event.registersList
                    if (event.registersList.isEmpty()) {
                        DialogHandler.showDialog(
                            message = registerError,
                            buttonText = "OK",
                            iconRes = R.drawable.info_icon
                        ) {}
                        // Clear selected register when no registers are available
                        selectedRegister = null
                    } else {
                        // Don't auto-select - user must explicitly select a register
                        selectedRegister = null
                    }
                }
            }
        }
    }

    // 🔹 UI Layout
    Box(modifier = Modifier.fillMaxSize()) {
        // 🟦 Background Image (Optional)
        Image(
            painter = painterResource(id = R.drawable.splash_background_image),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        HeaderAppBarAuth()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BaseText(
                    text = "Welcome ${viewModel.getName().replaceFirstChar { it.uppercase() }}!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 34F
                )

                Spacer(modifier = Modifier.height(10.dp))

                BaseText(
                    text = stringResource(id = R.string.you_re_almost_ready_to_start_your_shift_please_select_your_register_to_continue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18F,
                    textAlign = TextAlign.Center
                )
            }

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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))
                        // 🟦  Location Dropdown- Always visible
                        DropdownSelector(
                            label = stringResource(id = R.string.select_location),
                            items = locations.map { it.name ?: "" },
                            selectedText = if (locations.isEmpty()) {
                                stringResource(R.string.select_locations_hint)
                            } else {
                                selectedLocation?.name ?: locations.firstOrNull()?.name ?: ""
                            },
                            onItemSelected = { index ->
                                selectedLocation = locations[index]
                                viewModel.getStoreRegisters(locations[index].id)
                            })

                        Spacer(Modifier.height(16.dp))

                        // 🟧 Register Dropdown - Always visible
                                val registerDisplayText = if (registers.isEmpty()) {
                            selectRegisterHint
                        } else {
                            selectedRegister?.name ?: selectRegisterHint
                        }
                        
                        DropdownSelector(
                            label = stringResource(id = R.string.select_register),
                            items = registers.map { it.name ?: "" },
                            selectedText = registerDisplayText,
                            onItemSelected = { index ->
                                selectedRegister = registers[index]
                            })

                        Spacer(Modifier.height(40.dp))

                        BaseButton(
                            text = stringResource(id = R.string.button_continue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = isButtonEnabled
                        ) {
                            if (selectedRegister == null || registerDisplayText == selectRegisterHint) {
                                // Show error if register is not selected
                                DialogHandler.showDialog(
                                    message = selectRegisterErrorMessage,
                                    buttonText = "OK",
                                    iconRes = R.drawable.info_icon
                                ) {}
                            } else {
                                selectedLocation?.let { loc ->
                                    selectedRegister?.let { reg ->
                                        viewModel.getProducts(loc.id, reg.id, reg)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(5.dp))

                        //🔴 Logout Text Button
                        TextButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = stringResource(id = R.string.logout),
                                color = colorResource(id = R.color.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}
