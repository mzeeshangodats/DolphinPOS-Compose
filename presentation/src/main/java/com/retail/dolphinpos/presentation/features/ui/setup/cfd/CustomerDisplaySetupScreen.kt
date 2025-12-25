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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseOutlinedEditTextSmallHeight
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. IP Address Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(id = R.color.light_grey),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left: Blue IP icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ip_address),
                            contentDescription = "IP Address",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Center: Label and Note
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            BaseText(
                                text = "IP Address",
                                color = Color.Black,
                                fontSize = 14f,
                                fontFamily = GeneralSans,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            BaseText(
                                text = "Note: Port 8080 is used automatically",
                                color = Color.Gray,
                                fontSize = 12f,
                                fontFamily = GeneralSans
                            )
                        }
                        // Right: TextField
                        Box(
                            modifier = Modifier.weight(1.5f)
                        ) {
                            BaseOutlinedEditTextSmallHeight(
                                value = viewState.ipAddress,
                                onValueChange = { viewModel.updateIpAddress(it) },
                                placeholder = "127.0.0.1 or xxx.xxx.xxx.xxx",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 2. Enable Customer Display Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(id = R.color.color_grey1),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left: Blue IP icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ip_address),
                            contentDescription = "Enable Customer Display",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Center-left: Label
                        BaseText(
                            text = "Enable Customer Display",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Right: Toggle Switch
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

                    Spacer(modifier = Modifier.height(50.dp))

                    // 3. Bottom Buttons (aligned to end/right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BaseButton(
                            text = "Cancel",
                            modifier = Modifier
                                .height(45.dp),
                            backgroundColor = Color.White,
                            textColor = Color.Black,
                            border = BorderStroke(1.dp, colorResource(id = R.color.borderOutline)),
                            onClick = { viewModel.onCancel() }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        BaseButton(
                            text = "Save",
                            modifier = Modifier
                                .height(45.dp),
                            enabled = viewState.isButtonEnabled && !viewState.isLoading,
                            onClick = { viewModel.saveConfiguration() }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        BaseButton(
                            text = "Test Connection",
                            modifier = Modifier
                                .height(45.dp),
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

