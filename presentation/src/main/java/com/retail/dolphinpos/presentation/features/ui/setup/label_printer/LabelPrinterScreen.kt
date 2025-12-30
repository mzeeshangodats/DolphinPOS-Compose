package com.retail.dolphinpos.presentation.features.ui.setup.label_printer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditTextSmallHeight
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LabelPrinterScreen(
    navController: NavController,
    viewModel: LabelPrintingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showVariantDialog by remember { mutableStateOf(false) }
    var selectedProductForVariants by remember {
        mutableStateOf<com.retail.dolphinpos.domain.model.home.catrgories_products.Products?>(
            null
        )
    }
    var showPrintDialog by remember { mutableStateOf(false) }
    var discoveredPrinters by remember {
        mutableStateOf<List<com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo>>(
            emptyList()
        )
    }
    var selectedVariants by remember { mutableStateOf<List<LabelPrintingVariantModel>>(emptyList()) }

    // USB Permission handling
    val usbPermissionAction =
        "com.retail.dolphinpos.presentation.features.ui.setup.printer.USB_PERMISSION"
    var usbPermissionReceiver: BroadcastReceiver? by remember { mutableStateOf(null) }

    // Filter products based on search query
    val filteredProducts = remember(searchQuery, viewState.products) {
        if (searchQuery.isEmpty()) {
            viewState.products
        } else {
            viewState.products.filter {
                it.name?.contains(searchQuery, ignoreCase = true) == true ||
                        it.barCode?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    // Handle ViewEffects
    LaunchedEffect(Unit) {

        checkUsbPermission(context, viewModel, usbPermissionAction) { receiver ->
            usbPermissionReceiver = receiver
        }

        viewModel.viewEffect.collectLatest { effect ->
            when (effect) {
                is LabelPrintingViewEffect.NavigateToBack -> {
                    navController.navigateUp()
                }

                is LabelPrintingViewEffect.ShowErrorSnackBar -> {
                    DialogHandler.showDialog(
                        message = effect.message,
                        buttonText = "OK",
                        iconRes = R.drawable.cross_red
                    ) {}
                }

                is LabelPrintingViewEffect.ShowInformationSnackBar -> {
                    DialogHandler.showDialog(
                        message = effect.message,
                        buttonText = "OK",
                        iconRes = R.drawable.info_icon
                    ) {}
                }

                is LabelPrintingViewEffect.ShowSuccessSnackBar -> {
                    DialogHandler.showDialog(
                        message = effect.message,
                        buttonText = "OK",
                        iconRes = R.drawable.success_circle_icon
                    ) {}
                }

                is LabelPrintingViewEffect.Loading -> {
                    if (effect.isLoading) {
                        Loader.show("Please wait...")
                    } else {
                        Loader.hide()
                    }
                }

                is LabelPrintingViewEffect.ShowPrintDialog -> {
                    if (effect.printers.isNotEmpty()) {
                        discoveredPrinters = effect.printers
                        showPrintDialog = true
                    } else {
                        DialogHandler.showDialog(
                            message = "No printers found. Please connect a Brother printer via USB.",
                            buttonText = "OK",
                            iconRes = R.drawable.cross_red
                        ) {}
                    }
                }

                is LabelPrintingViewEffect.CheckAndSearchPrinters -> {
                    checkUsbPermission(context, viewModel, usbPermissionAction) { receiver ->
                        usbPermissionReceiver = receiver
                    }
                }

                is LabelPrintingViewEffect.ShowDialog -> {
                    DialogHandler.showDialog(
                        message = effect.message,
                        buttonText = "OK",
                        iconRes = R.drawable.cross_red
                    ) {}
                }
            }
        }
    }

    // Cleanup USB receiver
    DisposableEffect(Unit) {
        onDispose {
            usbPermissionReceiver?.let { receiver ->
                try {
                    context.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    // Already unregistered
                }
            }
            Loader.hide()
        }
    }

    // Update selected variants from viewState
    LaunchedEffect(viewState.selectedVariants) {
        selectedVariants = viewState.selectedVariants
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

    ) {
        // Header
        HeaderAppBarWithBack(
            title = "Label Printer Setup",
            onBackClick = navigateToHome
        )

        // Two cards side by side
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Card (40% width) - Product Selection
            Card(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, colorResource(id = R.color.stroke)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BaseText(
                        text = "Product",
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(id = R.color.black)
                    )

                    // Product Autocomplete
                    ProductAutocomplete(
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        products = filteredProducts,
                        onProductSelected = { product ->
                            searchQuery = ""
                            if (product.barCode?.isEmpty() == true) {
                                DialogHandler.showDialog(
                                    message = "Product has no barcode, cannot generate barcode",
                                    buttonText = "OK",
                                    iconRes = R.drawable.cross_red
                                ) {}
                                return@ProductAutocomplete
                            }
                            if (product.variants != null /*&& (product.variants.size<0)*/) {
                                selectedProductForVariants = product
                                showVariantDialog = true
                            } else {
                                // Add product directly if no variants
                                val variant = LabelPrintingVariantModel(
                                    productId = product.id,
                                    productName = product.name ?: "",
                                    variantId = null,
                                    variantName = null,
                                    barcode = product.barCode ?: "",
                                    quantity = 1
                                )
                                val updated = selectedVariants + variant
                                viewModel.onUpdateVariants(updated)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Print Button
                    BaseButton(
                        text = "Print",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        backgroundColor = colorResource(R.color.primary),
                        textColor = Color.White,
                        onClick = {
                            viewModel.onPrintClicked()
                        },
                        enabled = viewState.isPrintButtonEnabled
                    )
                }
            }

            // Right Card (60% width) - Barcode Labels Grid
            Card(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, colorResource(id = R.color.stroke)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(selectedVariants) { variant ->
                        BarcodeLabelItem(
                            variant = variant,
                            onRemove = {
                                val updated = selectedVariants.filter { it != variant }
                                viewModel.onUpdateVariants(updated)
                            }
                        )
                    }
                }
            }
        }
    }

    // Variant Selection Dialog
    if (showVariantDialog && selectedProductForVariants != null) {
        VariantSelectionDialog(
            product = selectedProductForVariants!!,
            variants = viewModel.mapToLabelPrintingVariants(selectedProductForVariants!!),
            onDismiss = { showVariantDialog = false },
            onVariantsSelected = { variants ->
                val updated = selectedVariants + variants
                viewModel.onUpdateVariants(updated)
                showVariantDialog = false
            }
        )
    }

    // Print Dialog
    if (showPrintDialog) {
        PrintDialog(
            printers = discoveredPrinters,
            onDismiss = { showPrintDialog = false },
            onPrinterSelected = { printer ->
                viewModel.onStartPrintingClicked(printer)
                showPrintDialog = false
            }
        )
    }
}

@Composable
fun ProductAutocomplete(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    products: List<com.retail.dolphinpos.domain.model.home.catrgories_products.Products>,
    onProductSelected: (com.retail.dolphinpos.domain.model.home.catrgories_products.Products) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    val filtered = remember(searchQuery, products) {
        if (searchQuery.length >= 3) {
            products.filter {
                it.name?.contains(searchQuery, ignoreCase = true) == true ||
                        it.barCode?.contains(searchQuery, ignoreCase = true) == true
            }
        } else {
            emptyList()
        }
    }

    Column {
        BaseOutlinedEditTextSmallHeight(
            value = searchQuery,
            onValueChange = { query ->
                onQueryChange(query)
                showDropdown = query.length >= 3 && filtered.isNotEmpty()
            },
            placeholder = "Choose Product",
            modifier = Modifier.fillMaxWidth()
        )

        // Dropdown
        if (showDropdown && filtered.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filtered.take(5).forEach { product ->
                        BaseText(
                            text = product.name ?: "Unknown Product",
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onProductSelected(product)
                                    onQueryChange("")
                                    showDropdown = false
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeLabelItem(
    variant: LabelPrintingVariantModel,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.white)),
        border = BorderStroke(1.dp, colorResource(id = R.color.borderOutline))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                Spacer(modifier = Modifier.height(20.dp))
                // Barcode info
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BaseText(
                            text = variant.productName,
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            color = Color.Black
                        )
                        variant.variantName?.let {
                            BaseText(
                                text = " - $it",
                                fontSize = 12f,
                                fontFamily = GeneralSans,
                                color = Color.Black,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Cash",
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            color = Color.Black
                        )
                        BaseText(
                            text = "Card",
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            color = Color.Black
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "\$${variant.cashPrice}",
                            fontSize = 16f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            color = Color.Black
                        )
                        BaseText(
                            text = "\$${variant.cardPrice}",
                            fontSize = 16f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            color = Color.Black
                        )
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_barcode_printer),
                        contentDescription = "barcode",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )


                    BaseText(
                        text = variant.barcode,
                        fontSize = 12f,
                        fontFamily = GeneralSans,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun VariantSelectionDialog(
    product: com.retail.dolphinpos.domain.model.home.catrgories_products.Products,
    variants: List<LabelPrintingVariantModel>,
    onDismiss: () -> Unit,
    onVariantsSelected: (List<LabelPrintingVariantModel>) -> Unit
) {
    var selectedVariants by remember { mutableStateOf<Set<Int>>(emptySet()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BaseText(
                        text = "  Select Variants",
                        fontSize = 18f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                // Variants List
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(variants.size) { index ->
                        val variant = variants[index]
                        val isSelected = selectedVariants.contains(index)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        selectedVariants = selectedVariants - index
                                    } else {
                                        selectedVariants = selectedVariants + index
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
                            ),
                            border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                BaseText(
                                    text = variant.variantName ?: variant.productName,
                                    fontSize = 14f,
                                    fontFamily = GeneralSans,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                                BaseText(
                                    text = "Barcode: ${variant.barcode}",
                                    fontSize = 12f,
                                    fontFamily = GeneralSans,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(44.dp)
                    ) {
                        BaseText(text = "Cancel", fontSize = 14f, fontFamily = GeneralSans, color = colorResource(R.color.gray_neutral))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val selected = variants.filterIndexed { index, _ ->
                                selectedVariants.contains(index)
                            }
                            onVariantsSelected(selected)
                        },
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary)
                        )
                    ) {
                        BaseText(
                            text = "Add",
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintDialog(
    printers: List<com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo>,
    onDismiss: () -> Unit,
    onPrinterSelected: (com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo) -> Unit
) {
    var selectedPrinter by remember {
        mutableStateOf<com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo?>(
            null
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.5f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BaseText(
                    text = "Select Printer",
                    fontSize = 18f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Bold
                )

                // Printer Dropdown
                var expanded by remember { mutableStateOf(false) }
                val printerNames = listOf("Select Printer") + printers.map { it.modelName }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPrinter?.modelName ?: "Select Printer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        printerNames.forEachIndexed { index, name ->
                            if (index == 0) {
                                DropdownMenuItem(
                                    text = {
                                        BaseText(
                                            text = name,
                                            fontSize = 14f,
                                            fontFamily = GeneralSans,
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        selectedPrinter = null
                                        expanded = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = {
                                        BaseText(
                                            text = name,
                                            fontSize = 14f,
                                            fontFamily = GeneralSans,
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        selectedPrinter = printers[index - 1]
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(44.dp)
                    ) {
                        BaseText(text = "Cancel", fontSize = 14f, fontFamily = GeneralSans, color = colorResource(R.color.gray_neutral))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            selectedPrinter?.let { onPrinterSelected(it) }
                        },
                        modifier = Modifier.height(44.dp),
                        enabled = selectedPrinter != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary)
                        )
                    ) {
                        BaseText(
                            text = "Print",
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

fun checkUsbPermission(
    context: Context,
    viewModel: LabelPrintingViewModel,
    permissionAction: String,
    onReceiverCreated: (BroadcastReceiver) -> Unit
) {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val brotherDevices = usbManager.deviceList.values.filter { it.vendorId == 0x04F9 }

    if (brotherDevices.isEmpty()) {
        DialogHandler.showDialog(
            message = "No Brother printer found. Please connect a Brother printer via USB.",
            buttonText = "OK",
            iconRes = R.drawable.cross_red
        ) {}
        return
    }

    // Check if any device needs permission
    val devicesNeedingPermission = brotherDevices.filter { !usbManager.hasPermission(it) }

    if (devicesNeedingPermission.isEmpty()) {
        // All devices have permission, proceed with search
        viewModel.startSearchUSBPrinter(context)
        return
    }

    // Request permission for the first device that needs it
    val deviceToRequest = devicesNeedingPermission.first()

    val filter = IntentFilter(permissionAction)
    val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == permissionAction) {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)

                if (granted && device != null) {
                    // Permission granted, now search for printers
                    viewModel.startSearchUSBPrinter(context)
                } else {

                    try {
                        if(usbManager.hasPermission(device)) {
                            viewModel.startSearchUSBPrinter(context)
                        } else {
                            // Permission denied
                            DialogHandler.showDialog(
                                message = "USB permission denied. Please grant USB permission to access the printer.",
                                buttonText = "OK",
                                iconRes = R.drawable.cross_red
                            ) {}
                        }

                    }catch ( e : Exception){
                        // ignore
                    }
                }

                try {
                    context?.unregisterReceiver(this)
                } catch (e: Exception) {
                    // Already unregistered
                }
            }
        }
    }

    ContextCompat.registerReceiver(
        context,
        usbPermissionReceiver,
        filter,
        ContextCompat.RECEIVER_NOT_EXPORTED
    )

    onReceiverCreated(usbPermissionReceiver)

    // Request permission
    val permissionIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(permissionAction).apply {
            putExtra(UsbManager.EXTRA_DEVICE, deviceToRequest)
        },
        PendingIntent.FLAG_IMMUTABLE
    )
    usbManager.requestPermission(deviceToRequest, permissionIntent)
}
