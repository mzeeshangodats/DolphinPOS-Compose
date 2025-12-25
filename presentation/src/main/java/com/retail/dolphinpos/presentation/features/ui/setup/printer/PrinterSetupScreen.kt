package com.retail.dolphinpos.presentation.features.ui.setup.printer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.DropdownSelector
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType as DomainPrinterConnectionType
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterViewEffect
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PrinterSetupScreen(
    navController: NavController,
    viewModel: PrinterSetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    
    // Map domain connection type to UI enum
    var connectionType by remember { mutableStateOf(PrinterConnectionType.LAN) }
    var selectedPrinter by remember { mutableStateOf<PrinterDetails?>(null) }
    var printerAddress by remember { mutableStateOf("") }
    
    // Track pending actions after permission grant
    var pendingDiscovery by remember { mutableStateOf(false) }
    var pendingTestPrint by remember { mutableStateOf(false) }
    
    // Bluetooth permission launcher
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val bluetoothConnectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        val bluetoothScanGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
        } else {
            true // Not needed for Android < 12
        }
        val granted = bluetoothConnectGranted && bluetoothScanGranted
        viewModel.updateBluetoothPermissionStatus(granted)
        
        // Execute pending actions after permission granted
        if (granted) {
            if (pendingDiscovery) {
                pendingDiscovery = false
                viewModel.startDiscovery(context, excludeBluetooth = connectionType != PrinterConnectionType.BLUETOOTH)
                selectedPrinter = null
                printerAddress = ""
            }
            if (pendingTestPrint) {
                pendingTestPrint = false
                viewModel.onTestPrintClicked()
            }
        }
    }
    
    // Check Bluetooth permissions on start
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBluetoothConnect = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val hasBluetoothScan = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            viewModel.updateBluetoothPermissionStatus(hasBluetoothConnect && hasBluetoothScan)
        } else {
            viewModel.updateBluetoothPermissionStatus(true)
        }
    }

    // Handle ViewEffects - Use DisposableEffect to clean up loader when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            // Hide loader when leaving this screen
            Loader.hide()
        }
    }

    // Handle ViewEffects
    LaunchedEffect(Unit) {
        viewModel.viewEffect.collectLatest { effect ->
            when (effect) {
                is PrinterViewEffect.ShowErrorSnackBar -> {
                    DialogHandler.showDialog(
                        message = effect.message,
                        buttonText = "OK",
                        iconRes = R.drawable.cross_red
                    ) {}
                }
                is PrinterViewEffect.ShowErrorDialog -> {
                    DialogHandler.showDialog(
                        message = effect.message,
                        buttonText = "OK",
                        iconRes = R.drawable.cross_red
                    ) {}
                }
                is PrinterViewEffect.ShowSuccessSnackBar -> {
                    DialogHandler.showDialog(
                        message = effect.message,
                        buttonText = "OK",
                        iconRes = R.drawable.success_circle_icon
                    ) {}
                }
                is PrinterViewEffect.ShowInformationSnackBar -> {
                    // Could use a snackbar here instead of dialog
                    DialogHandler.showDialog(
                        message = effect.message,
                        buttonText = "OK",
                        iconRes = R.drawable.info_icon
                    ) {}
                }
                is PrinterViewEffect.ShowLoading -> {
                    if (effect.isLoading) {
                        Loader.show("Discovering printers...")
                    } else {
                        Loader.hide()
                    }
                }
            }
        }
    }

    // Initialize connection type from saved printer on first load
    LaunchedEffect(viewState.savedPrinterDetails) {
        viewState.savedPrinterDetails?.let { printer ->
            if (connectionType == PrinterConnectionType.LAN) {
                connectionType = printer.connectionType.toUiConnectionType()
            }
        }
    }

    // Include saved printer in discovered printers if it exists and matches connection type
    val allPrintersWithSaved = remember(viewState.discoveredPrinters, viewState.savedPrinterDetails, connectionType) {
        val savedPrinter = viewState.savedPrinterDetails
        val printers = viewState.discoveredPrinters.toMutableList()
        
        // Add saved printer if it matches connection type and isn't already in the list
        savedPrinter?.let { printer ->
            if (printer.connectionType.toUiConnectionType() == connectionType) {
                val exists = printers.any { it.address == printer.address && it.name == printer.name }
                if (!exists) {
                    printers.add(0, printer) // Add at the beginning
                }
            }
        }
        printers
    }

    // Filter printers based on selected connection type
    val filteredPrinters = remember(connectionType, allPrintersWithSaved) {
        allPrintersWithSaved.filter { 
            it.connectionType.toUiConnectionType() == connectionType 
        }
    }

    // Initialize saved printer selection when filtered printers or saved printer changes
    LaunchedEffect(viewState.savedPrinterDetails, filteredPrinters) {
        viewState.savedPrinterDetails?.let { printer ->
            printerAddress = printer.address
            // Find printer in filtered list
            val foundPrinter = filteredPrinters.firstOrNull { 
                it.address == printer.address && it.name == printer.name 
            }
            if (foundPrinter != null && selectedPrinter?.address != foundPrinter.address) {
                selectedPrinter = foundPrinter
            } else if (foundPrinter == null && selectedPrinter != null) {
                selectedPrinter = null
                printerAddress = ""
            }
        } ?: run {
            // No saved printer, reset selection
            if (selectedPrinter != null) {
                selectedPrinter = null
                printerAddress = ""
            }
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
            title = "Printer Setup",
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
                    .fillMaxWidth(0.6f),
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
                    Spacer(modifier = Modifier.height(3.dp))

                    // 1. Printer Name Section
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
                        // Left: Blue printer icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_store),
                            contentDescription = "Printer Name",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Center: Label and Warning
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            BaseText(
                                text = "Printer Name",
                                color = Color.Black,
                                fontSize = 14f,
                                fontFamily = GeneralSans,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Warning message with icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_caution),
                                    contentDescription = "Warning",
                                    tint = Color(0xFFFFA500), // Yellow/orange color
                                    modifier = Modifier.size(16.dp)
                                )
                                BaseText(
                                    text = "No Printer found click \"Discover\" to search for printer!",
                                    color = Color.Gray,
                                    fontSize = 12f,
                                    fontFamily = GeneralSans
                                )
                            }
                        }
                        // Right: No Printer Selected field with Discover button inside
                        Box(
                            modifier = Modifier.weight(0.75f)
                        ) {
                            // No Printer Selected field
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(45.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                border = BorderStroke(1.dp, colorResource(R.color.color_grey))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Text on the left
                                    BaseText(
                                        text = selectedPrinter?.name ?: "No Printer Selected",
                                        color = if (selectedPrinter != null) Color.Black else Color.Gray,
                                        fontSize = 11f,
                                        fontFamily = GeneralSans,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = 12.dp, end = 80.dp)
                                    )
                                    // Discover button inside on the right
                                    BaseButton(
                                        text = "Discover",
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .height(38.dp)
                                            .padding(end = 4.dp),
                                        backgroundColor = colorResource(id = R.color.gray_neutral),
                                        textColor = Color.White,
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        fontSize = 12,
                                        onClick = { 
                                            // Check Bluetooth permission before starting discovery
                                            if (connectionType == PrinterConnectionType.BLUETOOTH || connectionType == PrinterConnectionType.LAN) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    val hasBluetoothConnect = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.BLUETOOTH_CONNECT
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    val hasBluetoothScan = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.BLUETOOTH_SCAN
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    
                                                    if (hasBluetoothConnect && hasBluetoothScan) {
                                                        viewModel.startDiscovery(context, excludeBluetooth = connectionType != PrinterConnectionType.BLUETOOTH)
                                                        selectedPrinter = null
                                                        printerAddress = ""
                                                    } else {
                                                        // Request permissions
                                                        pendingDiscovery = true
                                                        val permissions = mutableListOf<String>()
                                                        if (!hasBluetoothConnect) {
                                                            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                                                        }
                                                        if (!hasBluetoothScan) {
                                                            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                                                        }
                                                        if (permissions.isNotEmpty()) {
                                                            bluetoothPermissionLauncher.launch(permissions.toTypedArray())
                                                        }
                                                    }
                                                } else {
                                                    viewModel.startDiscovery(context, excludeBluetooth = connectionType != PrinterConnectionType.BLUETOOTH)
                                                    selectedPrinter = null
                                                    printerAddress = ""
                                                }
                                            } else {
                                                viewModel.startDiscovery(context, excludeBluetooth = true)
                                                selectedPrinter = null
                                                printerAddress = ""
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Connection Type Section
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
                        // Left: Blue printer icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_store),
                            contentDescription = "Connection Type",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Center-left: Label with asterisk
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
                            PrinterConnectionType.entries.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        connectionType = option
                                        selectedPrinter = null
                                        printerAddress = ""
                                    }
                                ) {
                                    RadioButton(
                                        selected = connectionType == option,
                                        onClick = {
                                            connectionType = option
                                            selectedPrinter = null
                                            printerAddress = ""
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = colorResource(id = R.color.primary),
                                            unselectedColor = Color.Gray
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    BaseText(
                                        text = option.displayName,
                                        color = Color.Black,
                                        fontSize = 12f,
                                        fontFamily = GeneralSans
                                    )
                                }
                            }
                        }
                    }

                    // 3. Auto Print Receipt Section
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
                        // Left: Blue printer icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_store),
                            contentDescription = "Auto Print Receipt",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Center-left: Label
                        BaseText(
                            text = "Auto Print Receipt",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Right: Toggle Switch
                        Switch(
                            checked = viewState.isAutoPrintEnabled,
                            onCheckedChange = { viewModel.updateAutoPrintEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colorResource(id = R.color.primary),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray
                            )
                        )
                    }

                    // 4. Auto Open Drawer Section
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
                        // Left: Blue drawer icon
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Auto Open Drawer",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Center-left: Label
                        BaseText(
                            text = "Auto Open Drawer",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Right: Toggle Switch
                        Switch(
                            checked = viewState.isAutoOpenDrawerEnabled,
                            onCheckedChange = { viewModel.updateAutoOpenDrawerEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colorResource(id = R.color.primary),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray
                            )
                        )
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
                            onClick = { navigateToHome() }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        BaseButton(
                            text = "Save",
                            modifier = Modifier
                                .height(45.dp),
                            onClick = { 
                                viewModel.onSaveClicked(
                                    isAutoPrintEnabled = viewState.isAutoPrintEnabled,
                                    isAutoOpenDrawerEnabled = viewState.isAutoOpenDrawerEnabled
                                )
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        BaseButton(
                            text = "Test Print",
                            modifier = Modifier
                                .height(45.dp),
                            fontSize = 14,
                            onClick = { 
                                // Check Bluetooth permission before test print if using Bluetooth
                                if (viewState.savedPrinterDetails?.connectionType == DomainPrinterConnectionType.BLUETOOTH) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val hasBluetoothConnect = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        ) == PackageManager.PERMISSION_GRANTED
                                        
                                        if (hasBluetoothConnect) {
                                            viewModel.onTestPrintClicked()
                                        } else {
                                            pendingTestPrint = true
                                            bluetoothPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                                        }
                                    } else {
                                        viewModel.onTestPrintClicked()
                                    }
                                } else {
                                    viewModel.onTestPrintClicked()
                                }
                            }
                        )
                    }
                }
            }
        }

        /*// Print Last Pending Order Button
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            BaseButton(
                text = "Print Last Pending Order",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                fontSize = 12,
                onClick = {
                    viewModel.onPrintLastPendingOrderClicked()
                }
            )
        }*/

        Spacer(modifier = Modifier.height(16.dp))
    }
}

enum class PrinterConnectionType(val displayName: String) {
    LAN("LAN"),
    BLUETOOTH("Bluetooth"),
    USB("USB")
}

// Extension functions to convert between domain and UI enums
private fun DomainPrinterConnectionType.toUiConnectionType(): PrinterConnectionType {
    return when (this) {
        DomainPrinterConnectionType.LAN -> PrinterConnectionType.LAN
        DomainPrinterConnectionType.BLUETOOTH -> PrinterConnectionType.BLUETOOTH
        DomainPrinterConnectionType.USB -> PrinterConnectionType.USB
        DomainPrinterConnectionType.UNKNOWN -> PrinterConnectionType.LAN // Default to LAN
    }
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
    placeholder: String,
    enabled: Boolean = true
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
                placeholder = placeholder,
                enabled = enabled
            )
        }
    }
}

