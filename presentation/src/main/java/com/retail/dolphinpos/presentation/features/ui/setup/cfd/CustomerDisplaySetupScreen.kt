package com.retail.dolphinpos.presentation.features.ui.setup.cfd

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader

@Composable
fun CustomerDisplaySetupScreen(
    navController: NavController,
    viewModel: CustomerDisplaySetupViewModel = hiltViewModel()
) {
    val viewState by viewModel.viewState.collectAsState()

    // Handle loading state
    LaunchedEffect(viewState.isLoading) {
        if (viewState.isLoading) {
            Loader.show("Processing...")
        } else {
            Loader.hide()
        }
    }

    // Handle error messages
    LaunchedEffect(viewState.errorMessage) {
        viewState.errorMessage?.let { message ->
            DialogHandler.showDialog(message = message, buttonText = "OK") {
                viewModel.clearMessages()
            }
        }
    }

    // Handle success messages
    LaunchedEffect(viewState.successMessage) {
        viewState.successMessage?.let { message ->
            DialogHandler.showDialog(
                message = message,
                buttonText = "OK",
                iconRes = R.drawable.success_circle_icon
            ) {
                viewModel.clearMessages()
            }
        }
    }

    // Handle navigation
    LaunchedEffect(viewState.shouldNavigateBack) {
        if (viewState.shouldNavigateBack) {
            navController.popBackStack()
            viewModel.clearNavigation()
        }
    }

    // Function to navigate to home
    val navigateToHome = {
        navController.navigate("home") {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
            .verticalScroll(rememberScrollState())
    ) {
        // Header with back button and title
        HeaderAppBarWithBack(
            title = "Customer Display Setup",
            onBackClick = navigateToHome
        )

        // Spacer for card positioning
        Spacer(modifier = Modifier.height(16.dp))

        // Centered Card with 4dp padding and 50% screen width
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.5f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Row 1: IP Address
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 10.dp)
                    ) {
                        BaseText(
                            text = "IP Address",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BaseOutlinedEditText(
                            value = viewState.ipAddress,
                            onValueChange = { viewModel.updateIpAddress(it) },
                            placeholder = "127.0.0.1 or xxx.xxx.xxx.xxx"
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BaseText(
                            text = "Note: Port 8080 is used automatically",
                            color = Color.Gray,
                            fontSize = 12f,
                            fontFamily = GeneralSans
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Enable Customer Display Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BaseText(
                            text = "Enable Customer Display",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = viewState.isEnabled,
                            onCheckedChange = { viewModel.updateEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colorResource(id = R.color.primary),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BaseButton(
                            text = "Cancel",
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            backgroundColor = Color.White,
                            textColor = Color.Black,
                            border = BorderStroke(1.dp, colorResource(id = R.color.borderOutline)),
                            onClick = { viewModel.onCancel() }
                        )

                        BaseButton(
                            text = "Save",
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            enabled = viewState.isButtonEnabled && !viewState.isLoading,
                            onClick = { viewModel.saveConfiguration() }
                        )

                        BaseButton(
                            text = "Test Connection",
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            fontSize = 14,
                            enabled = viewState.isButtonEnabled && !viewState.isLoading,
                            onClick = { viewModel.testConnection() }
                        )
                    }
                }
            }
        }
    }
}

