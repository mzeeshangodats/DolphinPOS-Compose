package com.retail.dolphinpos.presentation.features.ui.products

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.DropdownSelector
import com.retail.dolphinpos.common.components.HeaderAppBar
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.components.LogoutConfirmationDialog
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.common.utils.uriToFile
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import java.io.File
import java.io.FileOutputStream

@Composable
fun CreateProductScreen(
    navController: NavController,
    preferenceManager: PreferenceManager,
    onNavigateBack: () -> Unit,
    viewModel: CreateProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Get username and clock-in status from preferences
    val userName = preferenceManager.getName()
    val isClockedIn = preferenceManager.isClockedIn()
    val clockInTime = preferenceManager.getClockInTime()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is CreateProductUiEvent.ProductCreated -> {
                    DialogHandler.showDialog(
                        message = "Product created successfully!",
                        buttonText = "OK",
                        iconRes = R.drawable.success_circle_icon
                    ) {
                        navController.popBackStack()
                    }
                }
                is CreateProductUiEvent.ProductSynced -> {
                    // Product synced successfully
                }
                is CreateProductUiEvent.SyncFailed -> {
                    DialogHandler.showDialog(
                        message = "Product saved locally. ${event.message}",
                        buttonText = "OK"
                    ) {}
                }
                is CreateProductUiEvent.ShowError -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = "OK"
                    ) {}
                }

                is CreateProductUiEvent.NavigateToPinCode -> {
                    navController.navigate("pinCode") {
                        popUpTo(0) { inclusive = false }
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            Loader.show("Creating product...")
        } else {
            Loader.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header App Bar
        HeaderAppBarWithBack(
            title = "Create Product",
            onBackClick = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProductDetailsSection(
                            productName = uiState.productName,
                            barcode = uiState.barcode,
                            alternateBarcode = uiState.alternateBarcode,
                            description = uiState.description,
                            onProductNameChange = { viewModel.updateProductName(it) },
                            onBarcodeChange = { viewModel.updateBarcode(it) },
                            onBarcodeGenerate = { viewModel.generateProductBarcode() },
                            onAlternateBarcodeChange = { viewModel.updateAlternateBarcode(it) },
                            onDescriptionChange = { viewModel.updateDescription(it) }
                        )

                        InventorySection(
                            quantity = uiState.quantity,
                            trackQuantity = uiState.trackQuantity,
                            continueSellingWhenOutOfStock = uiState.continueSellingWhenOutOfStock,
                            hasSkuOrBarcode = uiState.hasSkuOrBarcode,
                            isEBTEligible = uiState.isEBTEligible,
                            onQuantityChange = { viewModel.updateQuantity(it) },
                            onTrackQuantityChange = { viewModel.updateTrackQuantity(it) },
                            onContinueSellingChange = { viewModel.updateContinueSellingWhenOutOfStock(it) },
                            onHasSkuOrBarcodeChange = { viewModel.updateHasSkuOrBarcode(it) },
                            onEBTEligibleChange = { viewModel.updateIsEBTEligible(it) }
                        )

                        ProductOrganizationSection(
                            productVendor = uiState.productVendor,
                            productVendorId = uiState.productVendorId,
                            vendors = uiState.vendors,
                            categoryName = uiState.categoryName,
                            categoryId = uiState.categoryId,
                            categories = uiState.categories,
                            selectedVariantType = uiState.selectedVariantType,
                            onProductVendorChange = { viewModel.updateProductVendor(it) },
                            onCategoryChange = { viewModel.updateCategory(it) },
                            onVariantTypeSelected = { viewModel.setSelectedVariantType(it) }
                        )
                    }

                    // Right Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PreviewImagesSection(
                            images = uiState.productImages,
                            onImageRemove = { viewModel.removeProductImage(it) },
                            onImageAdd = { url, name -> viewModel.addProductImage(url, name) },
                            context = LocalContext.current
                        )

                        PricingDetailsSection(
                            price = uiState.price,
                            compareAtPrice = uiState.compareAtPrice,
                            chargeTax = uiState.chargeTaxOnThisProduct,
                            costPerItem = uiState.costPerItem,
                            profit = uiState.profit,
                            margin = uiState.margin,
                            onPriceChange = { viewModel.updatePrice(it) },
                            onCompareAtPriceChange = { viewModel.updateCompareAtPrice(it) },
                            onChargeTaxChange = { viewModel.updateChargeTax(it) },
                            onCostPerItemChange = { viewModel.updateCostPerItem(it) },
                            onProfitChange = { viewModel.updateProfit(it) },
                            onMarginChange = { viewModel.updateMargin(it) }
                        )
                    }
                }
            }

            item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VariantsSection(
                            activeVariantTypes = uiState.activeVariantTypes,
                            sizeValues = uiState.sizeValues,
                            colorValues = uiState.colorValues,
                            customAttributes = uiState.customAttributes,
                            variants = uiState.variants,
                            onSizeValueAdded = { viewModel.addSizeValue(it) },
                            onSizeValueRemoved = { viewModel.removeSizeValue(it) },
                            onColorValueAdded = { viewModel.addColorValue(it) },
                            onColorValueRemoved = { viewModel.removeColorValue(it) },
                            onCustomAttributeAdded = { name, value -> viewModel.addCustomAttribute(name, value) },
                            onCustomAttributeRemoved = { name, value -> viewModel.removeCustomAttribute(name, value) },
                            onGenerateVariants = { viewModel.generateVariants() },
                            onVariantUpdate = { viewModel.updateVariant(it) },
                            onVariantRemove = { viewModel.removeVariant(it) },
                            context = LocalContext.current,
                            viewModel,
                        )
                    }

            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                // Save Button
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(48.dp)
                        .background(
                            color = colorResource(id = R.color.primary),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.createProduct() },
                    contentAlignment = Alignment.Center,

                ) {
                    BaseText(
                        text = "Save Product",
                        fontSize = 16f,
                        fontWeight = FontWeight.Medium,
                        fontFamily = GeneralSans,
                        color = Color.White
                    )
                }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            }
        )
    }
}

@Composable
fun ProductImageSection(
    onImageSelected: (String, String) -> Unit,
    context: android.content.Context
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract =  ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // ✅ USE uriToFile HERE
            val file = uriToFile(context, it)

            // Save file path + original name in state
            onImageSelected(
                file.absolutePath,
                file.name
            )
        }
    }
    
    Column {
        BaseText(
            text = "Product Image",
            fontSize = 14f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .padding(0.dp)
                .width(205.dp)
                .height(221.dp)
                .background(Color.White)
                .clickable { imagePickerLauncher.launch(PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bg_select_img),
                contentDescription = "image description",
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(205.dp)
                    .height(221.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_select_img),
                    contentDescription = "Camera",
                    modifier = Modifier
                        .width(56.dp)
                        .height(56.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                BaseText(
                    text = "Select image from\nyour device",
                    fontSize = 12f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    color = colorResource(R.color.primary),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ProductDetailsSection(
    productName: String,
    barcode: String,
    alternateBarcode: String,
    description: String,
    onProductNameChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onBarcodeGenerate: () -> Unit,
    onAlternateBarcodeChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BaseText(
            text = "Product Name*",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        BaseOutlinedEditText(
            value = productName,
            onValueChange = onProductNameChange,
            placeholder = "Enter name"
        )

        BaseText(
            text = "Barcode*",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        Box(modifier = Modifier.clickable { onBarcodeGenerate() }) {
            BaseOutlinedEditText(
                value = barcode,
                onValueChange = onBarcodeChange,
                placeholder = "Tap to generate",
                enabled = false
            )
        }

        BaseText(
            text = "Alternate Barcode",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        BaseOutlinedEditText(
            value = alternateBarcode,
            onValueChange = onAlternateBarcodeChange,
            placeholder = "Enter code"
        )

        BaseText(
            text = "Product Description*",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = { Text("Enter description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            singleLine = false,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(id = R.color.borderOutline),
                unfocusedBorderColor = colorResource(id = R.color.borderOutline)
            ),
            textStyle = TextStyle(
                fontFamily = GeneralSans,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
fun InventorySection(
    quantity: String,
    trackQuantity: Boolean,
    continueSellingWhenOutOfStock: Boolean,
    hasSkuOrBarcode: Boolean,
    isEBTEligible: Boolean,
    onQuantityChange: (String) -> Unit,
    onTrackQuantityChange: (Boolean) -> Unit,
    onContinueSellingChange: (Boolean) -> Unit,
    onHasSkuOrBarcodeChange: (Boolean) -> Unit,
    onEBTEligibleChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BaseText(
            text = "Quantity*",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        BaseOutlinedEditText(
            value = quantity,
            onValueChange = onQuantityChange,
            placeholder = "Enter quantity",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        CheckboxRow(
            text = "Track Quantity",
            checked = trackQuantity,
            onCheckedChange = onTrackQuantityChange
        )

        CheckboxRow(
            text = "Continue Selling When Out Of Stock",
            checked = continueSellingWhenOutOfStock,
            onCheckedChange = onContinueSellingChange
        )

        CheckboxRow(
            text = "This Product Has A SKU Or Barcode",
            checked = hasSkuOrBarcode,
            onCheckedChange = onHasSkuOrBarcodeChange
        )

        CheckboxRow(
            text = "This Product is EBT Eligible",
            checked = isEBTEligible,
            onCheckedChange = onEBTEligibleChange
        )
    }
}

@Composable
fun ProductOrganizationSection(
    productVendor: String,
    productVendorId: Int?,
    vendors: List<com.retail.dolphinpos.domain.model.product.VendorItem>,
    categoryName: String,
    categoryId: Int,
    categories: List<com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData>,
    selectedVariantType: String?,
    onProductVendorChange: (Int?) -> Unit,
    onCategoryChange: (Int?) -> Unit,
    onVariantTypeSelected: (String?) -> Unit
) {
    val variantTypeOptions = listOf("Size", "Color", "Custom")
    
    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownSelector(
            label = "Product Vendor*",
            items = vendors.map { it.title },
            selectedText = productVendor.ifEmpty { "--select vendor--" },
            onItemSelected = { index ->
                onProductVendorChange(vendors[index].id)
            }
        )

        DropdownSelector(
            label = "Choose Category*",
            items = categories.map { it.title },
            selectedText = categoryName.ifEmpty { "--select category--" },
            onItemSelected = { index ->
                onCategoryChange(categories[index].id)
            }
        )

        DropdownSelector(
            label = "Add Variant",
            items = variantTypeOptions,
            selectedText = selectedVariantType ?: "--select--",
            onItemSelected = { index ->
                onVariantTypeSelected(variantTypeOptions[index])
            }
        )
    }
}

@Composable
fun VariantsSection(
    activeVariantTypes: Set<String>,
    sizeValues: List<String>,
    colorValues: List<String>,
    customAttributes: List<Pair<String, String>>,
    variants: List<ProductVariantData>,
    onSizeValueAdded: (String) -> Unit,
    onSizeValueRemoved: (String) -> Unit,
    onColorValueAdded: (String) -> Unit,
    onColorValueRemoved: (String) -> Unit,
    onCustomAttributeAdded: (String, String) -> Unit,
    onCustomAttributeRemoved: (String, String) -> Unit,
    onGenerateVariants: () -> Unit,
    onVariantUpdate: (ProductVariantData) -> Unit,
    onVariantRemove: (ProductVariantData) -> Unit,
    context: android.content.Context,
    viewModel: CreateProductViewModel
) {
    var sizeInput by remember { mutableStateOf("") }
    var colorInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }
    var customValueInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Size Section - Show only if active
        if (activeVariantTypes.contains("Size")) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BaseText(
                    text = "Size",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BaseOutlinedEditText(
                        value = sizeInput,
                        onValueChange = { sizeInput = it },
                        placeholder = "Enter size",
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(48.dp)
                            .background(
                                color = colorResource(id = R.color.primary).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                if (sizeInput.isNotBlank()) {
                                    onSizeValueAdded(sizeInput)
                                    sizeInput = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = "Add Size",
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            color = colorResource(id = R.color.primary),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Size chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(sizeValues) { size ->
                        VariantChip(
                            text = size,
                            onRemove = { onSizeValueRemoved(size) }
                        )
                    }
                }
            }
        }

        // Color Section - Show only if active
        if (activeVariantTypes.contains("Color")) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BaseText(
                    text = "Color",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BaseOutlinedEditText(
                        value = colorInput,
                        onValueChange = { colorInput = it },
                        placeholder = "Enter color",
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(48.dp)
                            .background(
                                color = colorResource(id = R.color.primary).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                if (colorInput.isNotBlank()) {
                                    onColorValueAdded(colorInput)
                                    colorInput = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = "Add Color",
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            color = colorResource(id = R.color.primary),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Color chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(colorValues) { color ->
                        VariantChip(
                            text = color,
                            onRemove = { onColorValueRemoved(color) }
                        )
                    }
                }
            }
        }

        // Custom Section - Show only if active
        if (activeVariantTypes.contains("Custom")) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BaseText(
                    text = "Custom",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BaseOutlinedEditText(
                        value = customNameInput,
                        onValueChange = { customNameInput = it },
                        placeholder = "New",
                        modifier = Modifier.weight(1f)
                    )
                    BaseOutlinedEditText(
                        value = customValueInput,
                        onValueChange = { customValueInput = it },
                        placeholder = "Value",
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(48.dp)
                            .background(
                                color = colorResource(id = R.color.primary).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                if (customNameInput.isNotBlank() && customValueInput.isNotBlank()) {
                                    onCustomAttributeAdded(customNameInput, customValueInput)
                                    customNameInput = ""
                                    customValueInput = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = "Add Custom",
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            color = colorResource(id = R.color.primary),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Custom chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(customAttributes) { (name, value) ->
                        VariantChip(
                            text = "$name $value",
                            onRemove = { onCustomAttributeRemoved(name, value) }
                        )
                    }
                }
            }
        }

        // Generate Variant Button
        if (sizeValues.isNotEmpty() || colorValues.isNotEmpty() || customAttributes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(48.dp)
                        .background(
                            color = colorResource(id = R.color.primary).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onGenerateVariants() },
                    contentAlignment = Alignment.Center
                ) {
                    BaseText(
                        text = "Generate Variant",
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        color = colorResource(id = R.color.primary),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Variants Table
        if (variants.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Table Header - Full width grey bar
                VariantTableHeader()
                
                // Variant Rows
                variants.forEach { variant ->
                    VariantTableRow(
                        variant = variant,
                        onUpdate = onVariantUpdate,
                        onRemove = { onVariantRemove(variant) },
                        onImageUpload = { file, originalName ->
                            viewModel.uploadVariantImage(variant, file, originalName)
                        },
                        context = context,
                        viewModel = viewModel
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun VariantChip(
    text: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = colorResource(id = R.color.primary).copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BaseText(
            text = text,
            fontSize = 12f,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.close_icon),
                contentDescription = "Remove",
                modifier = Modifier.size(12.dp),
                tint = colorResource(id = R.color.primary)
            )
        }
    }
}

@Composable
fun VariantTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorResource(R.color.primary))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BaseText(
            text = "Variant",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1.5f)
        )
        BaseText(
            text = "Image",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1f)
        )
        BaseText(
            text = "SKU",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1f)
        )
        BaseText(
            text = "Price",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1f)
        )
        BaseText(
            text = "Dual Price Card",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1f)
        )
        BaseText(
            text = "Cost Price",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1f)
        )
        BaseText(
            text = "Quantity",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1f)
        )
        BaseText(
            text = "Variant Barcode",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1.5f)
        )
        BaseText(
            text = "",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
fun VariantTableRow(
    variant: ProductVariantData,
    onUpdate: (ProductVariantData) -> Unit,
    onRemove: () -> Unit,
    onImageUpload: (File, String) -> Unit,
    context: android.content.Context,
    viewModel: CreateProductViewModel
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            onImageUpload(file, file.name)
        }
    }
    
    // Get first image URL for display
    val displayImageUrl = variant.images?.firstOrNull()?.fileURL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Variant Name
        BaseText(
            text = variant.title,
            fontSize = 12f,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1.5f),
            color = colorResource(R.color.grey_text_colour)
        )
        
        // Image
        Box(
            modifier = Modifier
                .weight(1f)
                .size(60.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (displayImageUrl != null) {
                AsyncImage(
                    model = displayImageUrl,
                    contentDescription = "Variant image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.cart_icon),
                        contentDescription = "Upload",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Gray
                    )
                    BaseText(
                        text = "Upload",
                        fontSize = 10f,
                        fontFamily = GeneralSans,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // SKU
        BaseOutlinedEditText(
            value = variant.sku,
            onValueChange = { onUpdate(variant.copy(sku = it)) },
            placeholder = "SKU",
            modifier = Modifier.weight(1f)
        )
        
        // Price
        BaseOutlinedEditText(
            value = variant.price,
            onValueChange = { onUpdate(variant.copy(price = it)) },
            placeholder = "0.0",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        
        // Dual Price Card (uneditable, auto-calculated)
        BaseOutlinedEditText(
            value = variant.dualPrice,
            onValueChange = {},
            placeholder = "0.0",
            enabled = false,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        
        // Cost Price
        BaseOutlinedEditText(
            value = variant.costPrice,
            onValueChange = { onUpdate(variant.copy(costPrice = it)) },
            placeholder = "0.0",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        
        // Quantity
        BaseOutlinedEditText(
            value = variant.quantity,
            onValueChange = { onUpdate(variant.copy(quantity = it)) },
            placeholder = "0",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        // Variant Barcode
        Column(
            modifier = Modifier.weight(1.5f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        viewModel.generateVariantBarcode(variant)
                    }
            ) {
                BaseOutlinedEditText(
                    value = variant.barcode,
                    onValueChange = { onUpdate(variant.copy(barcode = it)) },
                    placeholder = "Tap to generate",
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Delete Icon
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "Delete variant",
                modifier = Modifier.size(24.dp),
                tint = colorResource(id = R.color.primary)
            )
        }
    }
}

@Composable
fun VariantRow(
    variant: ProductVariantData,
    onRemove: () -> Unit,
    onUpdate: (ProductVariantData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BaseText(
                text = variant.title,
                fontSize = 12f,
                fontFamily = GeneralSans,
                color = Color.Black
            )
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(id = R.drawable.close_icon),
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BaseText(
                    text = "SKU",
                    fontSize = 10f,
                    fontFamily = GeneralSans,
                    color = Color.Gray
                )
                BaseOutlinedEditText(
                    value = variant.sku,
                    onValueChange = { onUpdate(variant.copy(sku = it)) },
                    placeholder = "Enter SKU",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                BaseText(
                    text = "Price",
                    fontSize = 10f,
                    fontFamily = GeneralSans,
                    color = Color.Gray
                )
                BaseOutlinedEditText(
                    value = variant.price,
                    onValueChange = { onUpdate(variant.copy(price = it)) },
                    placeholder = "$ 0.00",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Similar rows for Cost Price, Quantity, Barcode
    }
}

@Composable
fun PreviewImagesSection(
    images: List<ProductImageData>,
    onImageRemove: (ProductImageData) -> Unit,
    onImageAdd: (String, String) -> Unit,
    context: android.content.Context
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            onImageAdd(file.absolutePath, file.name)
        }
    }
    
    Column {
        BaseText(
            text = "Preview Images",
            fontSize = 14f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add image button as first item
            item {
                Box(
                    modifier = Modifier
                        .width(151.dp)
                        .height(138.dp)
                        .clickable { 
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                ) {
                    // Border image background
                    Icon(
                        painter = painterResource(id = R.drawable.bg_select_img),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Content with 2dp inner padding
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_select_img),
                            contentDescription = "Camera",
                            modifier = Modifier.size(32.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BaseText(
                            text = "Select image from\nyour device",
                            fontSize = 10f,
                            fontFamily = GeneralSans,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            items(images) { image ->
                Box(
                    modifier = Modifier
                        .width(151.dp)
                        .height(138.dp)

                ) {
                    // Border image background
                    Icon(
                        painter = painterResource(id = R.drawable.bg_select_img),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Image content with 2dp inner padding
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        AsyncImage(
                            model = if (image.url.startsWith("http")) {
                                image.url
                            } else {
                                File(image.url)
                            },
                            contentDescription = "Product image",
                            modifier = Modifier.width(135.dp)
                                .height(133.dp),
                            //contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Close icon at top right, slightly outside the border
                    IconButton(
                        onClick = { onImageRemove(image) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 9.dp, y = (-20).dp)
                            //.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Remove",
                            modifier = Modifier.size(24.dp),
                            tint = colorResource(id = R.color.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PricingDetailsSection(
    price: String,
    compareAtPrice: String,
    chargeTax: Boolean,
    costPerItem: String,
    profit: String,
    margin: String,
    onPriceChange: (String) -> Unit,
    onCompareAtPriceChange: (String) -> Unit,
    onChargeTaxChange: (Boolean) -> Unit,
    onCostPerItemChange: (String) -> Unit,
    onProfitChange: (String) -> Unit,
    onMarginChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BaseText(
            text = "Price*",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        BaseOutlinedEditText(
            value = price,
            onValueChange = onPriceChange,
            placeholder = "Enter price",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        BaseText(
            text = "Dual Price Card*",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        BaseOutlinedEditText(
            value = compareAtPrice,
            onValueChange = {},
            placeholder = "Auto-calculated",
            enabled = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        CheckboxRow(
            text = "Charge Tax On This Product",
            checked = chargeTax,
            onCheckedChange = onChargeTaxChange
        )

        BaseText(
            text = "Cost Per Item*",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        BaseOutlinedEditText(
            value = costPerItem,
            onValueChange = onCostPerItemChange,
            placeholder = "Enter price",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        BaseText(
            text = "Profit*",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        BaseOutlinedEditText(
            value = profit,
            onValueChange = {},
            placeholder = "Auto-calculated",
            enabled = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        BaseText(
            text = "Markup %",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        BaseOutlinedEditText(
            value = margin,
            onValueChange = {},
            placeholder = "Auto-calculated",
            enabled = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}

@Composable
fun CheckboxRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(20.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = colorResource(id = R.color.primary)
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        BaseText(
            text = text,
            fontSize = 12f,
            fontFamily = GeneralSans,
            color = Color.Black,
            modifier = Modifier.clickable { onCheckedChange(!checked) }
        )
    }
}

