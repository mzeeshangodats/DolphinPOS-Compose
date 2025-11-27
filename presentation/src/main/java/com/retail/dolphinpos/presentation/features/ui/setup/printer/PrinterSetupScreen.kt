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
                    // Row 1: Printer Name - Selected Printer Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 10.dp)
                    ) {
                        BaseText(
                            text = "Printer Name",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Selected Printer Card
                        selectedPrinter?.let { printer ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorResource(id = R.color.primary).copy(alpha = 0.1f)
                                ),
                                border = BorderStroke(1.dp, colorResource(id = R.color.primary))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    BaseText(
                                        text = printer.name,
                                        color = Color.Black,
                                        fontSize = 14f,
                                        fontFamily = GeneralSans,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    // Only show address if not USB printer
                                    if (printer.connectionType != DomainPrinterConnectionType.USB) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        BaseText(
                                            text = printer.address,
                                            color = Color.Gray,
                                            fontSize = 12f,
                                            fontFamily = GeneralSans
                                        )
                                    }
                                }
                            }
                        } ?: run {
                            // No printer selected message
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                border = BorderStroke(1.dp, Color.Gray)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    BaseText(
                                        text = "No printer selected",
                                        color = Color.Gray,
                                        fontSize = 14f,
                                        fontFamily = GeneralSans,
                                    )
                                }
                            }
                        }
                        
                        // Printers List
                        if (filteredPrinters.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            BaseText(
                                text = "Available Printers",
                                color = Color.Black,
                                fontSize = 12f,
                                fontFamily = GeneralSans,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp), // Fixed height for the list
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredPrinters) { printer ->
                                        val isSelected = selectedPrinter?.address == printer.address && 
                                                         selectedPrinter?.name == printer.name
                                        
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .clickable {
                                                    selectedPrinter = printer
                                                    printerAddress = printer.address
                                                    viewModel.onDeviceClicked(printer)
                                                },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) {
                                                    colorResource(id = R.color.primary).copy(alpha = 0.15f)
                                                } else {
                                                    Color.White
                                                }
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) colorResource(id = R.color.primary) else Color(0xFFE0E0E0)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    BaseText(
                                                        text = printer.name,
                                                        color = if (isSelected) colorResource(id = R.color.primary) else Color.Black,
                                                        fontSize = 14f,
                                                        fontFamily = GeneralSans,
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                    // Only show address if not USB printer
                                                    if (printer.connectionType != DomainPrinterConnectionType.USB) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        BaseText(
                                                            text = printer.address,
                                                            color = Color.Gray,
                                                            fontSize = 12f,
                                                            fontFamily = GeneralSans
                                                        )
                                                    }
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.success_circle_icon),
                                                        contentDescription = "Selected",
                                                        tint = colorResource(id = R.color.primary),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty state
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    BaseText(
                                        text = "No printers found. Click 'Discover' to search for printers.",
                                        color = Color.Gray,
                                        fontSize = 12f,
                                        fontFamily = GeneralSans,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Connection Type
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        BaseText(
                            text = "Connection Type",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PrinterConnectionType.entries.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        connectionType = option
                                        // Reset selected printer when connection type changes
                                        selectedPrinter = null
                                        printerAddress = ""
                                    }
                                ) {
                                    RadioButton(
                                        selected = connectionType == option,
                                        onClick = {
                                            connectionType = option
                                            // Reset selected printer when connection type changes
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
                                        fontSize = 14f,
                                        fontFamily = GeneralSans
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 3: Printer Address
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        BaseText(
                            text = "Address",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BaseOutlinedEditText(
                            value = printerAddress,
                            onValueChange = { printerAddress = it },
                            placeholder = "Enter printer address",
                            enabled = selectedPrinter == null // Allow editing only when no printer is selected
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 4: Auto Print Receipt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BaseText(
                            text = "Auto Print Receipt",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 5: Auto Open Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BaseText(
                            text = "Auto Open Drawer",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
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
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    fontSize = 12,
                    onClick = { navigateToHome() }
                )

                BaseButton(
                    text = "Save",
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    fontSize = 12,
                    onClick = { 
                        viewModel.onSaveClicked(
                            isAutoPrintEnabled = viewState.isAutoPrintEnabled,
                            isAutoOpenDrawerEnabled = viewState.isAutoOpenDrawerEnabled
                        )
                    }
                )

                BaseButton(
                    text = "Discover",
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
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

                BaseButton(
                    text = "Test Print",
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    fontSize = 12,
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

