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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseOutlinedEditTextSmallHeight
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.DropdownSelector
import com.retail.dolphinpos.common.components.DropdownSelectorSmallHeight
import com.retail.dolphinpos.common.components.HeaderAppBar
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.components.LogoutConfirmationDialog
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.common.utils.uriToFile
import com.retail.dolphinpos.data.util.safeApiCall
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import java.io.File
import java.io.FileOutputStream

/**
 * Filters input to only allow digits and one dot (for decimal numbers)
 * Removes commas, minus signs, and other special characters
 */
private fun filterNumericInput(input: String): String {
    var hasDot = false
    return input.filter { char ->
        when {
            char.isDigit() -> true
            char == '.' && !hasDot -> {
                hasDot = true
                true
            }

            else -> false // Filter out commas, minus, and other special characters
        }
    }
}

@Composable
fun CreateProductScreen(
    navController: NavController,
    preferenceManager: PreferenceManager,
    productId: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: CreateProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Product Details") } // Default to "Product Details"

    // Load product data if productId is provided (update mode)
    LaunchedEffect(productId) {
        productId?.let { id ->
            viewModel.loadProduct(id)
        }
    }

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

                is CreateProductUiEvent.ProductUpdated -> {
                    DialogHandler.showDialog(
                        message = "Product updated successfully!",
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
                    // Sync failed but product is saved locally - don't show error
                    // Success dialog is already shown via ProductCreated event
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
            .background(colorResource(id = R.color.light_grey))
    ) {
        // Header App Bar
        HeaderAppBarWithBack(
            title = if (productId != null) "Update Product" else "Create Product",
            onBackClick = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Tab Bar + Content (Left Column + Right Column)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tab Bar (Half Width, Right Aligned)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.25f)
                            .align(Alignment.End),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TabBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            modifier = Modifier.weight(0.5f)
                        )
                    }

                    // Content based on selected tab
                    if (selectedTab == "Product Details") {
                        // Show all cards: Left Column + Right Column
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Column: Create Product + Inventory + Product Organization
                            Column(
                                modifier = Modifier.weight(0.5f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Create Product Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        BaseText(
                                            text = "Product Information",
                                            fontSize = 18f,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = GeneralSans,
                                            color = Color.Black
                                        )

                                        // Product Details (full width, no image section)
                                        ProductDetailsSection(
                                            productName = uiState.productName,
                                            barcode = uiState.barcode,
                                            sku = uiState.sku,
                                            alternateBarcode = uiState.alternateBarcode,
                                            plu = uiState.plu,
                                            description = uiState.description,
                                            onProductNameChange = { viewModel.updateProductName(it) },
                                            onBarcodeChange = { viewModel.updateBarcode(it) },
                                            onBarcodeGenerate = { viewModel.generateProductBarcode() },
                                            onSkuChange = { viewModel.updateSku(it) },
                                            onAlternateBarcodeChange = {
                                                viewModel.updateAlternateBarcode(
                                                    it
                                                )
                                            },
                                            onPluChange = { viewModel.updatePlu(it) },
                                            onDescriptionChange = { viewModel.updateDescription(it) }
                                        )
                                    }
                                }

                                // Inventory Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        BaseText(
                                            text = "Inventory",
                                            fontSize = 18f,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = GeneralSans,
                                            color = Color.Black,
                                            modifier = Modifier.padding(
                                                bottom = 12.dp,
                                                start = 9.dp
                                            )
                                        )
                                        InventorySection(
                                            quantity = uiState.quantity,
                                            trackQuantity = uiState.trackQuantity,
                                            continueSellingWhenOutOfStock = uiState.continueSellingWhenOutOfStock,
                                            hasSkuOrBarcode = uiState.hasSkuOrBarcode,
                                            isEBTEligible = uiState.isEBTEligible,
                                            onQuantityChange = { viewModel.updateQuantity(it) },
                                            onTrackQuantityChange = {
                                                viewModel.updateTrackQuantity(
                                                    it
                                                )
                                            },
                                            onContinueSellingChange = {
                                                viewModel.updateContinueSellingWhenOutOfStock(
                                                    it
                                                )
                                            },
                                            onHasSkuOrBarcodeChange = {
                                                viewModel.updateHasSkuOrBarcode(
                                                    it
                                                )
                                            },
                                            onEBTEligibleChange = { viewModel.updateIsEBTEligible(it) }
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }

                                // Product Organization Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        BaseText(
                                            text = "Product Organization",
                                            fontSize = 18f,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = GeneralSans,
                                            color = Color.Black,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        ProductOrganizationSection(
                                            productVendor = uiState.productVendor,
                                            productVendorId = uiState.productVendorId,
                                            vendors = uiState.vendors,
                                            categoryName = uiState.categoryName,
                                            categoryId = uiState.categoryId,
                                            categories = uiState.categories,
                                            onProductVendorChange = {
                                                viewModel.updateProductVendor(
                                                    it
                                                )
                                            },
                                            onCategoryChange = { viewModel.updateCategory(it) }
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                    }
                                }
                            }

                            // Right Column: Preview Images + Pricing Details
                            Column(
                                modifier = Modifier.weight(0.5f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Preview Images Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        PreviewImagesSection(
                                            images = uiState.productImages,
                                            onImageRemove = { viewModel.removeProductImage(it) },
                                            onImageAdd = { url, name ->
                                                viewModel.addProductImage(
                                                    url,
                                                    name
                                                )
                                            },
                                            context = LocalContext.current
                                        )
                                    }
                                }

                                // Dual Pricing Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        BaseText(
                                            text = "Dual Pricing",
                                            fontSize = 18f,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = GeneralSans,
                                            color = Color.Black,
                                            modifier = Modifier.padding(
                                                bottom = 12.dp,
                                                start = 9.dp
                                            )
                                        )
                                        DualPricingSection(
                                            price = uiState.price,
                                            compareAtPrice = uiState.compareAtPrice,
                                            chargeTax = uiState.chargeTaxOnThisProduct,
                                            onPriceChange = { viewModel.updatePrice(it) },
                                            onCompareAtPriceChange = {
                                                viewModel.updateCompareAtPrice(
                                                    it
                                                )
                                            },
                                            onChargeTaxChange = { viewModel.updateChargeTax(it) }
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }

                                // Pricing Details Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        BaseText(
                                            text = "Pricing Details",
                                            fontSize = 18f,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = GeneralSans,
                                            color = Color.Black,
                                            modifier = Modifier.padding(
                                                bottom = 12.dp,
                                                start = 9.dp
                                            )
                                        )
                                        PricingDetailsSection(
                                            costPerItem = uiState.costPerItem,
                                            profit = uiState.profit,
                                            margin = uiState.margin,
                                            onCostPerItemChange = { viewModel.updateCostPerItem(it) },
                                            onProfitChange = { viewModel.updateProfit(it) },
                                            onMarginChange = { viewModel.updateMargin(it) }
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }

                                // Label Printer Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        BaseText(
                                            text = "Print Label",
                                            fontSize = 18f,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = GeneralSans,
                                            color = Color.Black,
                                            modifier = Modifier.padding(
                                                bottom = 12.dp,
                                                start = 9.dp
                                            )
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            modifier = Modifier
                                                .clickable { /* send action */ }
                                                .padding(top = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(0.5f)
                                            ) {
                                                // Enable Label Printing checkbox
                                                CheckboxRow(
                                                    text = "Enable Label Printing",
                                                    checked = uiState.enableLabelPrinting,
                                                    onCheckedChange = {
                                                        viewModel.updateEnableLabelPrinting(
                                                            it
                                                        )
                                                    }
                                                )
                                            }

                                            // Warning message with icon
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_caution),
                                                    contentDescription = "Warning",
                                                    tint = Color.Unspecified,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                BaseText(
                                                    text = "Label Printing Will Not Work Without A Barcode.",
                                                    fontSize = 11f,
                                                    fontFamily = GeneralSans,
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Light
                                                )
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Variant tab: Show only Variants Card (Full Width)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VariantsSection(
                                    selectedVariantType = uiState.selectedVariantType,
                                    activeVariantTypes = uiState.activeVariantTypes,
                                    sizeValues = uiState.sizeValues,
                                    colorValues = uiState.colorValues,
                                    customAttributes = uiState.customAttributes,
                                    variants = uiState.variants,
                                    onVariantTypeSelected = { viewModel.setSelectedVariantType(it) },
                                    onSizeValueAdded = { viewModel.addSizeValue(it) },
                                    onSizeValueRemoved = { viewModel.removeSizeValue(it) },
                                    onColorValueAdded = { viewModel.addColorValue(it) },
                                    onColorValueRemoved = { viewModel.removeColorValue(it) },
                                    onCustomAttributeAdded = { name, value ->
                                        viewModel.addCustomAttribute(
                                            name,
                                            value
                                        )
                                    },
                                    onCustomAttributeRemoved = { name, value ->
                                        viewModel.removeCustomAttribute(
                                            name,
                                            value
                                        )
                                    },
                                    onGenerateVariants = { viewModel.generateVariants() },
                                    onVariantUpdate = { viewModel.updateVariant(it) },
                                    onVariantRemove = { viewModel.removeVariant(it) },
                                    context = LocalContext.current,
                                    viewModel,
                                )
                            }
                        }
                    }
                }
            }


            // Save Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(48.dp)
                            .background(
                                color = if (uiState.isLoading)
                                    colorResource(id = R.color.primary).copy(alpha = 0.6f)
                                else
                                    colorResource(id = R.color.primary),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !uiState.isLoading) {
                                if (!uiState.isLoading) {
                                    viewModel.createProduct()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = if (uiState.isLoading) "Saving..." else if (productId != null) "Update Product" else "Save Product",
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
        contract = ActivityResultContracts.PickVisualMedia()
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
                .clickable {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
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
    sku: String,
    alternateBarcode: String,
    plu: String,
    description: String,
    onProductNameChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onBarcodeGenerate: () -> Unit,
    onSkuChange: (String) -> Unit,
    onAlternateBarcodeChange: (String) -> Unit,
    onPluChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Product Name
                BaseText(
                    text = "Product Name*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                BaseOutlinedEditTextSmallHeight(
                    value = productName,
                    onValueChange = onProductNameChange,
                    placeholder = "Enter name"
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Barcode
                BaseText(
                    text = "Barcode*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                Box(modifier = Modifier.clickable { onBarcodeGenerate() }) {
                    BaseOutlinedEditTextSmallHeight(
                        value = barcode,
                        onValueChange = onBarcodeChange,
                        placeholder = "Tap to generate",
                        enabled = false
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // SKU
                BaseText(
                    text = "SKU*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                BaseOutlinedEditTextSmallHeight(
                    value = sku,
                    onValueChange = onSkuChange,
                    placeholder = "Enter SKU"
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        // Alternate Barcode and PLU (side by side, 50% each)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Alternate Barcode (50% width)
            Column(
                modifier = Modifier.weight(0.5f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    BaseText(
                        text = "Alternate Barcode",
                        fontSize = 12f,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = GeneralSans,
                        color = colorResource(id = R.color.primary)
                    )
                    IconButton(
                        onClick = { /* Add alternate barcode action */ },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.add_quantity),
                            contentDescription = "Add",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                BaseOutlinedEditTextSmallHeight(
                    value = alternateBarcode,
                    onValueChange = onAlternateBarcodeChange,
                    placeholder = "Enter code",
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.image_bar_code_scan),
                            contentDescription = "Scan Barcode",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }

            // PLU (50% width)
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(top = 8.dp)
            ) {
                BaseText(
                    text = "PLU*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                BaseOutlinedEditTextSmallHeight(
                    value = plu,
                    onValueChange = onPluChange,
                    placeholder = "Enter PLU",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Product Description (below PLU)
        BaseText(
            text = "Product Description*",
            fontSize = 12f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = { Text("Enter description", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(bottom = 15.dp),
            singleLine = false,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(id = R.color.borderOutline),
                unfocusedBorderColor = colorResource(id = R.color.borderOutline)
            ),
            textStyle = TextStyle(
                fontFamily = GeneralSans,
                fontSize = 12.sp
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
        // Row 1: Quantity (half width) + Continue Selling checkbox (right side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Quantity field (half width)
            Column(
                modifier = Modifier.weight(0.5f),
            ) {
                BaseText(
                    text = "Quantity*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                BaseOutlinedEditTextSmallHeight(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    placeholder = "Enter quantity",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Right: Continue Selling checkbox + SKU/Barcode checkbox
            Column(
                modifier = Modifier.weight(0.5f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp)) // Align with quantity field
                CheckboxRow(
                    text = "Continue Selling When Out Of Stock",
                    checked = continueSellingWhenOutOfStock,
                    onCheckedChange = onContinueSellingChange
                )

                Spacer(modifier = Modifier.height(15.dp))

                // This Product Has A SKU Or Barcode checkbox (below Continue Selling)
                CheckboxRow(
                    text = "This Product Has A SKU Or Barcode",
                    checked = hasSkuOrBarcode,
                    onCheckedChange = onHasSkuOrBarcodeChange
                )
            }
        }

        // Row 2: Track Quantity checkbox (below quantity field, left side)
        CheckboxRow(
            text = "Track Quantity",
            checked = trackQuantity,
            onCheckedChange = onTrackQuantityChange
        )

        // EBT Eligible checkbox
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
    onProductVendorChange: (Int?) -> Unit,
    onCategoryChange: (Int?) -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownSelectorSmallHeight(
            label = "Product Vendor*",
            items = vendors.map { it.title },
            selectedText = productVendor.ifEmpty { "--select vendor--" },
            onItemSelected = { index ->
                onProductVendorChange(vendors[index].id)
            }
        )

        DropdownSelectorSmallHeight(
            label = "Choose Category*",
            items = categories.map { it.title },
            selectedText = categoryName.ifEmpty { "--select category--" },
            onItemSelected = { index ->
                onCategoryChange(categories[index].id)
            }
        )
    }
}

@Composable
fun VariantsSection(
    selectedVariantType: String?,
    activeVariantTypes: Set<String>,
    sizeValues: List<String>,
    colorValues: List<String>,
    customAttributes: List<Pair<String, String>>,
    variants: List<ProductVariantData>,
    onVariantTypeSelected: (String?) -> Unit,
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

    val variantTypeOptions = listOf("Size", "Color", "Custom")
    // Variant Type Dropdown using DropdownSelector
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(start = 8.dp)
            .fillMaxSize(0.38f)
    ) {
        DropdownSelectorSmallHeight(
            label = "Add Variant",
            items = variantTypeOptions,
            selectedText = selectedVariantType ?: "--select--",
            onItemSelected = { index ->
                onVariantTypeSelected(variantTypeOptions[index])
            },
        )
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(start = 8.dp)
            .fillMaxSize(0.5f)
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
                    BaseOutlinedEditTextSmallHeight(
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
                    BaseOutlinedEditTextSmallHeight(
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
                    BaseOutlinedEditTextSmallHeight(
                        value = customNameInput,
                        onValueChange = { customNameInput = it },
                        placeholder = "New",
                        modifier = Modifier.weight(1f)
                    )
                    BaseOutlinedEditTextSmallHeight(
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
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
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
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1.5f),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "Variant",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "Image",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "SKU",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "Cash Price",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "Card Price",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "Cost Price",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "Quantity",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.weight(1.5f),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "Variant Barcode",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            // Empty space for delete icon
        }
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
            color = colorResource(R.color.grey_text_colour),
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        // Image
        Box(
            modifier = Modifier
                .weight(1f)
                .size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    // .weight(1f)
                    //.size(60.dp)
                    .height(60.dp)
                    .width(60.dp)
                    .border(0.5.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .background(color = colorResource(R.color.light_grey)),
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
                            painter = painterResource(id = R.drawable.ic_select_img),
                            contentDescription = "Upload",
                            modifier = Modifier.size(24.dp),
                            tint = colorResource(R.color.primary)
                        )
                        BaseText(
                            text = "Upload\nImage",
                            fontSize = 10f,
                            fontFamily = GeneralSans,
                            color = colorResource(R.color.primary),
                            maxLines = 2,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 12.sp
                            )
                        )
                    }
                }
            }
        }


        // SKU
        BaseOutlinedEditTextSmallHeight(
            value = variant.sku,
            onValueChange = { onUpdate(variant.copy(sku = it)) },
            placeholder = "SKU",
            modifier = Modifier.weight(1f)
        )


        // Price
        BaseOutlinedEditTextSmallHeight(
            value = variant.price,
            onValueChange = {
                // Filter input: only allow digits and one dot
                val filtered = filterNumericInput(it)
                onUpdate(variant.copy(price = filtered))
            },
            placeholder = "\$0.00",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // Dual Price Card (uneditable, auto-calculated)
        BaseOutlinedEditTextSmallHeight(
            value = variant.dualPrice,
            onValueChange = {},
            placeholder = "\$0.00",
            enabled = false,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // Cost Price
        BaseOutlinedEditTextSmallHeight(
            value = variant.costPrice,
            onValueChange = {
                // Filter input: only allow digits and one dot
                val filtered = filterNumericInput(it)
                onUpdate(variant.copy(costPrice = filtered))
            },
            placeholder = "\$0.00",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // Quantity
        BaseOutlinedEditTextSmallHeight(
            value = variant.quantity,
            onValueChange = {
                // Filter input: only allow digits (no decimal for quantity)
                val filtered = it.filter { char -> char.isDigit() }
                onUpdate(variant.copy(quantity = filtered))
            },
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
                BaseOutlinedEditTextSmallHeight(
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
            fontSize = 18f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp, start = 10.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),

            ) {
            // Add image button as first item
            item {
                Box(
                    modifier = Modifier
                        .width(125.dp)
                        .height(110.dp)
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
                            color = colorResource(R.color.primary),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 12.sp
                            )
                        )
                    }
                }
            }

            // All selected pictures appear after the "Select image" box
            items(images) { image ->
                Box(
                    modifier = Modifier
                        .width(125.dp)
                        .height(110.dp)

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
                            modifier = Modifier
                                .width(135.dp)
                                .height(133.dp),
                            //contentScale = ContentScale.Crop
                        )
                    }

                    // Close icon at top right, slightly outside the border
                    IconButton(
                        onClick = { onImageRemove(image) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 11.dp, y = (-22).dp)
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
fun DualPricingSection(
    price: String,
    compareAtPrice: String,
    chargeTax: Boolean,
    onPriceChange: (String) -> Unit,
    onCompareAtPriceChange: (String) -> Unit,
    onChargeTaxChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First Row: Cash Price (left) and Card Price (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Cash Price field
            Column(
                modifier = Modifier.weight(0.5f),
            ) {
                BaseText(
                    text = "Cash Price*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                BaseOutlinedEditTextSmallHeight(
                    value = price,
                    onValueChange = onPriceChange,
                    placeholder = "\$0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Right: Card Price field
            Column(
                modifier = Modifier.weight(0.5f),
            ) {
                BaseText(
                    text = "Card Price*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                BaseOutlinedEditTextSmallHeight(
                    value = compareAtPrice,
                    onValueChange = {},
                    placeholder = "\$0.00",
                    enabled = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))
        // Checkbox: Charge Tax On This Product (below Cash Price field, left side)
        CheckboxRow(
            text = "Charge Tax On This Product",
            checked = chargeTax,
            onCheckedChange = onChargeTaxChange
        )
    }
}

@Composable
fun PricingDetailsSection(
    costPerItem: String,
    profit: String,
    margin: String,
    onCostPerItemChange: (String) -> Unit,
    onProfitChange: (String) -> Unit,
    onMarginChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row: Cost Per Item (left), Profit (middle), Margin (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Cost Per Item
            Column(
                modifier = Modifier.weight(1f),
            ) {
                BaseText(
                    text = "Cost Per Item*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                BaseOutlinedEditTextSmallHeight(
                    value = costPerItem,
                    onValueChange = onCostPerItemChange,
                    placeholder = "Enter price",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Middle: Profit
            Column(
                modifier = Modifier.weight(1f),
            ) {
                BaseText(
                    text = "Profit*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                BaseOutlinedEditTextSmallHeight(
                    value = profit,
                    onValueChange = {},
                    placeholder = "Enter price",
                    enabled = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Right: Margin
            Column(
                modifier = Modifier.weight(1f),
            ) {
                BaseText(
                    text = "Margin*",
                    fontSize = 12f,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneralSans,
                    color = colorResource(id = R.color.primary)
                )
                BaseOutlinedEditTextSmallHeight(
                    value = margin,
                    onValueChange = {},
                    placeholder = "Enter price",
                    enabled = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }
}

@Composable
fun TabBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = colorResource(id = R.color.color_grey), // Use color_grey as background
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Product Details Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = if (selectedTab == "Product Details") Color.White else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { onTabSelected("Product Details") }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "Product Details",
                fontSize = 14f,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneralSans,
                color = if (selectedTab == "Product Details")
                    colorResource(id = R.color.primary)
                else
                    Color(0xFF666666) // Dark grey for unselected
            )
        }

        // Variant Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = if (selectedTab == "Variant") Color.White else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { onTabSelected("Variant") }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = "Variant",
                fontSize = 14f,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneralSans,
                color = if (selectedTab == "Variant")
                    colorResource(id = R.color.primary)
                else
                    Color(0xFF666666) // Dark grey for unselected
            )
        }
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

