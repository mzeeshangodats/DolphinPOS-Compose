package com.retail.dolphinpos.presentation.features.ui.setup.cc_processing

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseOutlinedEditTextSmallHeight
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.DropdownSelector
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
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
            title = "Credit Card Processing",
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
                    // 1. Card Provider Section
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
                        // Left: Blue card icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_card),
                            contentDescription = "Card Provider",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Center-left: Label
                        BaseText(
                            text = "Card Provider",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Right: Dropdown
                        Box(
                            modifier = Modifier.weight(1.5f)
                        ) {
                            InlineDropdownSelector(
                                items = TerminalType.entries.map { it.displayName },
                                selectedText = configState.selectedTerminalType.displayName,
                                onItemSelected = { index ->
                                    viewModel.updateTerminalType(TerminalType.entries[index])
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 2. Connection Type Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(id = R.color.color_grey1),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left: Blue pen/pencil icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_connection_type),
                            contentDescription = "Connection Type",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Label with asterisk
                        BaseText(
                            text = "Connection Type*",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Right: Radio Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CommunicationType.entries.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        viewModel.updateCommunicationType(option)
                                    }
                                ) {
                                    RadioButton(
                                        selected = configState.communicationType == option,
                                        onClick = { viewModel.updateCommunicationType(option) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = colorResource(id = R.color.primary),
                                            unselectedColor = Color.Gray
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    BaseText(
                                        text = option.displayName,
                                        color = Color.Black,
                                        fontSize = 14f,
                                        fontFamily = GeneralSans
                                    )
                                }
                            }
                        }
                    }

                    // 3. IP Address Section
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
                        // Center-left: Label
                        BaseText(
                            text = "IP Address",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Right: TextField
                        Box(
                            modifier = Modifier.weight(1.5f)
                        ) {
                            BaseOutlinedEditTextSmallHeight(
                                value = configState.ipAddress,
                                onValueChange = { viewModel.updateIpAddress(it) },
                                placeholder = "xxx.xxx.xxx.xxx",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 4. Port No Section
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
                        // Left: Blue port icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_port_no),
                            contentDescription = "Port No",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Center-left: Label
                        BaseText(
                            text = "Port No",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Right: TextField
                        Box(
                            modifier = Modifier.weight(1.5f)
                        ) {
                            BaseOutlinedEditTextSmallHeight(
                                value = configState.portNumber.ifEmpty { "10009" },
                                onValueChange = { viewModel.updatePortNumber(it) },
                                placeholder = "Enter Port Number",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 5. Bottom Buttons (aligned to end/right)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineDropdownSelector(
    items: List<String>,
    selectedText: String,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            textStyle = TextStyle(
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Light,
                fontSize = 11.sp
            ),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(51.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedBorderColor = colorResource(id = R.color.borderOutline),
                unfocusedBorderColor = colorResource(id = R.color.borderOutline),
                cursorColor = colorResource(id = R.color.primary)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            style = TextStyle(
                                fontFamily = GeneralSans,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    },
                    onClick = {
                        onItemSelected(index)
                        expanded = false
                    }
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

@Composable
private fun SettingRowWithRadioButtons(
    icon: Int,
    label: String,
    selectedOption: CommunicationType,
    options: List<CommunicationType>,
    onOptionSelected: (CommunicationType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.primary).copy(alpha = 0.1f)
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

        Spacer(modifier = Modifier.width(8.dp))

        // Label and Radio Buttons
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BaseText(
                text = label,
                color = Color.Black,
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOptionSelected(option) }
                    ) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = { onOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colorResource(id = R.color.primary),
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BaseText(
                            text = option.displayName,
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRowWithDropdown(
    icon: Int = R.drawable.card_icon,
    label: String = "Card Provider",
    selectedText: String = "",
    items: List<String> = emptyList(),
    onItemSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.primary).copy(alpha = 0.1f)
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

        Spacer(modifier = Modifier.width(8.dp))

        // Label and Dropdown
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BaseText(
                text = label,
                color = Color.Black,
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            DropdownSelector(
                label = "",
                items = items,
                selectedText = selectedText,
                onItemSelected = onItemSelected
            )
        }
    }
}

@Composable
private fun SettingRowWithSwitch(
    icon: Int,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
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
                    containerColor = colorResource(id = R.color.primary).copy(alpha = 0.1f)
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

        // Switch
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

@Composable
private fun SettingRowWithEditText(
    icon: Int,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.primary).copy(alpha = 0.1f)
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

        Spacer(modifier = Modifier.width(8.dp))

        // Label and EditText
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BaseText(
                text = label,
                color = Color.Black,
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            BaseOutlinedEditText(
                modifier = Modifier.weight(.5f),
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder
            )
        }
    }
}

