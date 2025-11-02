package com.retail.dolphinpos.presentation.features.ui.setup.payment

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
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.DropdownSelector
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader

@Composable
fun CreditCardProcessingScreen(
    navController: NavController,
    viewModel: CreditCardProcessingViewModel = hiltViewModel()
) {
    val viewState by viewModel.viewState.collectAsState()
    val configState = viewState.config

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
            DialogHandler.showDialog(message = message, buttonText = "OK") {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        BaseText(
            text = "Credit Card Processing",
            color = Color.Black,
            fontSize = 24f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp, 16.dp, 16.dp, 8.dp)
        )

        // Main Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Terminal Settings Section
                SettingRow(
                    icon = R.drawable.card_icon,
                    label = "Terminal Type",
                    content = {
                        DropdownSelector(
                            label = "",
                            items = TerminalType.entries.map { it.displayName },
                            selectedText = configState.selectedTerminalType.displayName,
                            onItemSelected = { index ->
                                viewModel.updateTerminalType(TerminalType.entries[index])
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Show fields based on terminal type
                when (configState.selectedTerminalType) {

                    TerminalType.EMV, TerminalType.PAX_A35, TerminalType.PAX_A920 -> {
                        // IP Address
                        SettingRow(
                            icon = R.drawable.card_icon,
                            label = "IP Address",
                            content = {
                                BaseOutlinedEditText(
                                    value = configState.ipAddress,
                                    onValueChange = { viewModel.updateIpAddress(it) },
                                    placeholder = "Enter IP Address"
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Port Number
                        SettingRow(
                            icon = R.drawable.card_icon,
                            label = "Port Number",
                            content = {
                                BaseOutlinedEditText(
                                    value = configState.portNumber,
                                    onValueChange = { viewModel.updatePortNumber(it) },
                                    placeholder = "Enter Port Number"
                                )
                            }
                        )

                        if (configState.selectedTerminalType == TerminalType.PAX_A35
                            || configState.selectedTerminalType == TerminalType.PAX_A920) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Bluetooth Address
                            SettingRow(
                                icon = R.drawable.card_icon,
                                label = "Bluetooth Address",
                                content = {
                                    BaseOutlinedEditText(
                                        value = configState.bluetoothAddress,
                                        onValueChange = { viewModel.updateBluetoothAddress(it) },
                                        placeholder = "Enter Bluetooth Address"
                                    )
                                }
                            )
                        }
                    }

                    TerminalType.WIFI, TerminalType.D200 -> {
                        // Terminal ID
                        SettingRow(
                            icon = R.drawable.card_icon,
                            label = "Terminal ID",
                            content = {
                                BaseOutlinedEditText(
                                    value = configState.terminalId,
                                    onValueChange = { viewModel.updateTerminalId(it) },
                                    placeholder = "Enter Terminal ID"
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Merchant ID
                        SettingRow(
                            icon = R.drawable.card_icon,
                            label = "Merchant ID",
                            content = {
                                BaseOutlinedEditText(
                                    value = configState.merchantId,
                                    onValueChange = { viewModel.updateMerchantId(it) },
                                    placeholder = "Enter Merchant ID"
                                )
                            }
                        )
                    }

                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Information Section
                SettingRow(
                    icon = R.drawable.card_icon,
                    label = "Phone Number",
                    content = {
                        BaseOutlinedEditText(
                            value = configState.phoneNumber,
                            onValueChange = { viewModel.updatePhoneNumber(it) },
                            placeholder = "Enter Phone Number"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingRow(
                    icon = R.drawable.card_icon,
                    label = "Email",
                    content = {
                        BaseOutlinedEditText(
                            value = configState.email,
                            onValueChange = { viewModel.updateEmail(it) },
                            placeholder = "Enter Email"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))

                // Receipt Options Section
                SettingRowWithToggle(
                    icon = R.drawable.card_icon,
                    label = "Email Receipt",
                    checked = configState.enableEmailReceipt,
                    onCheckedChange = { viewModel.updateEmailReceipt(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingRowWithToggle(
                    icon = R.drawable.card_icon,
                    label = "SMS Receipt",
                    checked = configState.enableSmsReceipt,
                    onCheckedChange = { viewModel.updateSmsReceipt(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        onClick = { viewModel.saveConfiguration() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                BaseButton(
                    text = "Test Connection",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = { viewModel.testConnection() }
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: Int,
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.primary).copy(
                    alpha = 0.1f
                )
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = label,
                    tint = colorResource(id = R.color.primary),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Label and Content
        Column(modifier = Modifier.weight(1f)) {
            BaseText(
                text = label,
                color = Color.Black,
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun SettingRowWithToggle(
    icon: Int,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.primary).copy(
                        alpha = 0.1f
                    )
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = label,
                        tint = colorResource(id = R.color.primary),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Label
            BaseText(
                text = label,
                color = Color.Black,
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Toggle Switch
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colorResource(id = R.color.primary),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray
            )
        )
    }
}

