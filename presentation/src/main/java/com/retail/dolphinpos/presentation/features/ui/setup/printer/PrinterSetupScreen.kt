package com.retail.dolphinpos.presentation.features.ui.setup.printer

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
import androidx.navigation.NavController
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.DropdownSelector
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R

@Composable
fun PrinterSetupScreen(
    navController: NavController
) {
    // Local state for printer configuration
    var selectedPrinterName by remember { mutableStateOf("No printer selected") }
    var connectionType by remember { mutableStateOf(PrinterConnectionType.LAN) }
    var printerAddress by remember { mutableStateOf("") }
    var isAutoPrintEnabled by remember { mutableStateOf(false) }
    var isAutoOpenDrawerEnabled by remember { mutableStateOf(false) }
    var isGraphicPrinterEnabled by remember { mutableStateOf(false) }

    // Dummy list of discovered printers
    val discoveredPrinters = remember { listOf("Printer 1", "Printer 2", "Printer 3") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .background(colorResource(id = R.color.light_grey))
            .verticalScroll(rememberScrollState())
    ) {
        // Heading
        BaseText(
            text = "Printer Setup",
            color = Color.Black,
            fontSize = 24f,
            fontFamily = GeneralSans,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
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
                    // Row 1: Printer Name
                    SettingRowWithDropdown(
                        icon = R.drawable.card_icon,
                        label = "Printer Name",
                        selectedText = selectedPrinterName,
                        items = listOf("No printer selected") + discoveredPrinters,
                        onItemSelected = { index ->
                            if (index == 0) {
                                selectedPrinterName = "No printer selected"
                            } else {
                                selectedPrinterName = discoveredPrinters[index - 1]
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 2: Connection Type
                    SettingRowWithRadioButtons(
                        icon = R.drawable.card_icon,
                        label = "Connection Type",
                        selectedOption = connectionType,
                        options = PrinterConnectionType.entries,
                        onOptionSelected = { connectionType = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 3: Printer Address
                    SettingRowWithEditText(
                        icon = R.drawable.card_icon,
                        label = "Address",
                        value = printerAddress,
                        onValueChange = { printerAddress = it },
                        placeholder = "Enter printer address"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 4: Auto Print Receipt
                    SettingRowWithSwitch(
                        icon = R.drawable.card_icon,
                        label = "Auto Print Receipt",
                        checked = isAutoPrintEnabled,
                        onCheckedChange = { isAutoPrintEnabled = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 5: Auto Open Drawer
                    SettingRowWithSwitch(
                        icon = R.drawable.card_icon,
                        label = "Auto Open Drawer",
                        checked = isAutoOpenDrawerEnabled,
                        onCheckedChange = { isAutoOpenDrawerEnabled = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 6: Graphic Printer
                    SettingRowWithSwitch(
                        icon = R.drawable.card_icon,
                        label = "Graphic Printer",
                        checked = isGraphicPrinterEnabled,
                        onCheckedChange = { isGraphicPrinterEnabled = it }
                    )
                }
            }
        }

        // Spacer after card
        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.5f),
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
                    onClick = { navController.popBackStack() }
                )

                BaseButton(
                    text = "Save",
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = { /* TODO: Save printer configuration */ }
                )

                BaseButton(
                    text = "Discover",
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    fontSize = 14,
                    onClick = { /* TODO: Start printer discovery */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

enum class PrinterConnectionType(val displayName: String) {
    LAN("LAN"),
    BLUETOOTH("Bluetooth"),
    USB("USB")
}

@Composable
private fun SettingRowWithRadioButtons(
    icon: Int,
    label: String,
    selectedOption: PrinterConnectionType,
    options: List<PrinterConnectionType>,
    onOptionSelected: (PrinterConnectionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
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

        // Label and Radio Buttons
        Column(modifier = Modifier.weight(1f)) {
            BaseText(
                text = label,
                color = Color.Black,
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
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
    label: String = "Printer Name",
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

