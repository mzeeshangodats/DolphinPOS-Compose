package com.retail.dolphinpos.presentation.features.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HomeAppBar
import com.retail.dolphinpos.common.components.LogoutConfirmationDialog
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import com.retail.dolphinpos.domain.model.home.cart.DiscountType
import com.retail.dolphinpos.domain.usecases.tax.PricingSummaryUseCase
import com.retail.dolphinpos.domain.model.home.cart.getProductDiscountedPrice
import com.retail.dolphinpos.domain.model.home.bottom_nav.BottomMenu
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Helper function to show "Coming Soon" dialog
 */
fun showComingSoonDialog() {
    DialogHandler.showDialog(
        message = "This feature is currently under development.",
        buttonText = "OK",
        iconRes = R.drawable.info_icon
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    var showOrderDiscountDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showHoldCartDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPaymentSuccessDialog by remember { mutableStateOf(false) }
    var showPLUSearchDialog by remember { mutableStateOf(false) }
    var paymentSuccessAmount by remember { mutableStateOf("0.00") }
    var amountTendered by remember { mutableStateOf(0.0) }
    var selectedProductForVariant by remember { mutableStateOf<Products?>(null) }
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val subtotal by viewModel.subtotal.collectAsStateWithLifecycle()
    val tax by viewModel.tax.collectAsStateWithLifecycle()
    val cashTotal by viewModel.cashTotal.collectAsStateWithLifecycle()
    val cardTotal by viewModel.cardTotal.collectAsStateWithLifecycle()
    val cashDiscountTotal by viewModel.cashDiscountTotal.collectAsStateWithLifecycle()
    val orderDiscountTotal by viewModel.orderDiscountTotal.collectAsStateWithLifecycle()
    val orderLevelDiscounts by viewModel.orderLevelDiscounts.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchProductResults.collectAsStateWithLifecycle()
    val holdCartCount by viewModel.holdCartCount.collectAsStateWithLifecycle()
    val pluSearchResult by viewModel.pluSearchResult.collectAsStateWithLifecycle()
    val isSearchingPLU by viewModel.isSearchingPLU.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf<CategoryData?>(null) }
    var paymentAmount by remember { mutableStateOf("0.00") }
    var searchQuery by remember { mutableStateOf("") }

    // Barcode scanner state
    var barcodeInput by remember { mutableStateOf("") }
    var lastBarcodeScanTime by remember { mutableStateOf(0L) }
    val barcodeFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var barcodeScanJob by remember { mutableStateOf<Job?>(null) }

    // Get username and clock-in status from preferences
    val userName = preferenceManager.getName()
    val isClockedIn = preferenceManager.isClockedIn()
    val clockInTime = preferenceManager.getClockInTime()

    // Handle UI events - Use DisposableEffect to clean up loader when leaving screen
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    DisposableEffect(currentRoute) {
        onDispose {
            // Hide loader when leaving this screen
            Loader.hide()
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == "home") {
            viewModel.homeUiEvent.collect { event ->
                // Double-check we're still on home screen before processing events
                if (navController.currentBackStackEntry?.destination?.route == "home") {
                    when (event) {
                        is HomeUiEvent.ShowLoading -> Loader.show("Please wait...")
                        is HomeUiEvent.HideLoading -> Loader.hide()
                        is HomeUiEvent.ShowError -> {
                            DialogHandler.showDialog(
                                message = event.message, buttonText = "OK"
                            ) {}
                        }

                        is HomeUiEvent.ShowSuccess -> {
                            DialogHandler.showDialog(
                                message = event.message,
                                buttonText = "OK",
                                iconRes = R.drawable.success_circle_icon
                            ) {}
                        }

                        is HomeUiEvent.HoldCartSuccess -> {
                            DialogHandler.showDialog(
                                message = event.message,
                                buttonText = "OK",
                                iconRes = R.drawable.cart_icon_blue
                            ) {}
                        }

                        is HomeUiEvent.PopulateCategoryList -> {
                            if (event.categoryList.isNotEmpty()) {
                                selectedCategory = event.categoryList[0]
                                viewModel.loadProducts(event.categoryList[0].id)
                            }
                        }

                        is HomeUiEvent.OrderCreatedSuccessfully -> {
                            // Store amount tendered (change) and show PaymentSuccessUI dialog
                            paymentSuccessAmount =
                                viewModel.formatAmount(amountTendered).replace("$", "")
                                    .replace(",", "")
                            showPaymentSuccessDialog = true
                        }

                        HomeUiEvent.NavigateToPinCode -> {
                            navController.navigate("pinCode") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }

    // Auto-select first category
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategory == null) {
            selectedCategory = categories[0]
            viewModel.loadProducts(categories[0].id)
        }
    }

    // Clear payment input only when cart becomes empty
    LaunchedEffect(cardTotal) {
        // Only clear payment input when cart is empty, don't auto-populate when items are added
        if (cardTotal <= 0) {
            paymentAmount = "0.00"
        }
    }

    // Load hold carts when screen loads
    LaunchedEffect(Unit) {
        viewModel.loadHoldCarts()
    }

    // Barcode scanner detection - debounce input to detect complete barcode scans
    LaunchedEffect(barcodeInput) {
        // Cancel previous job if input changed
        barcodeScanJob?.cancel()

        if (barcodeInput.isNotEmpty() && barcodeInput.length >= 3) {
            // Wait for 300ms after last input to detect if scanning is complete
            barcodeScanJob = coroutineScope.launch {
                delay(300)
                // Check if input hasn't changed (scanner finished)
                if (barcodeInput.isNotEmpty() && barcodeInput.length >= 3) {
                    // This looks like a complete barcode scan
                    viewModel.handleBarcodeScan(barcodeInput.trim())
                    barcodeInput = ""
                }
            }
        }
    }

    // Auto-focus barcode input field on screen load
    LaunchedEffect(Unit) {
        delay(100)
        barcodeFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
    ) {
        // Hidden barcode input field (for keyboard-based barcode scanners)
        // This field captures barcode scanner input (scanners act as HID keyboards)
        BasicTextField(
            value = barcodeInput,
            onValueChange = { newValue ->
                barcodeInput = newValue
                lastBarcodeScanTime = System.currentTimeMillis()
            },
            modifier = Modifier
                .size(1.dp) // Hidden field (1x1 dp, invisible)
                .offset(x = (-1000).dp, y = (-1000).dp) // Move off-screen
                .focusRequester(barcodeFocusRequester)
                .focusable(),
            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text, imeAction = ImeAction.None
            )
        )
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Home App Bar with search and logout
            HomeAppBar(
                searchQuery = searchQuery, onSearchQueryChange = { query ->
                    searchQuery = query
                    viewModel.searchProducts(query)
                }, onLogout = {
                    showLogoutDialog = true
                }, searchResults = searchResults, onProductClick = { product ->
                    val success = viewModel.addToCart(product)
                    if (success) {
                        searchQuery = ""
                    } else {
                        DialogHandler.showDialog("You can't add product after applying cash discount. If you want to add click on card first")
                    }
                }, userName = userName, isClockedIn = isClockedIn, clockInTime = clockInTime
            )

            Row(
                modifier = Modifier.weight(1f)
            ) {
                // Column 1 - Cart (always visible)
                CartPanel(
                    modifier = Modifier.weight(0.28f),
                    cartItems = cartItems,
                    holdCartCount = holdCartCount,
                    onRemoveFromCart = { productId, variantId ->
                        val success = viewModel.removeFromCart(productId, variantId)
                        if (!success) {
                            DialogHandler.showDialog("You can't remove item from cart after applying cash discount. If you want to remove click on card first")
                        }
                    },
                    onUpdateCartItem = { viewModel.updateCartItem(it) },
                    onAddCustomer = {
                        showAddCustomerDialog = true
                    },
                    onHoldCartClick = { showHoldCartDialog = true },
                    canApplyProductDiscount = { viewModel.canApplyProductDiscount() },
                    canRemoveItemFromCart = { viewModel.canRemoveItemFromCart() })

                // Column 2 - Pricing/Payment/Keypad + Action Buttons (25% width, full height)
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .background(colorResource(id = R.color.light_grey))
                        .padding(8.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        // Pricing Summary
                        PricingSummary(
                            subtotal = subtotal,
                            cashDiscountTotal = cashDiscountTotal,
                            orderDiscountTotal = orderDiscountTotal,
                            tax = tax,
                            cashTotal = cashTotal,
                            cardTotal = cardTotal,
                            isCashSelected = viewModel.isCashSelected,
                            cartItems = cartItems,
                            pricingSummaryUseCase = viewModel.pricingSummaryUseCase
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cart Action Buttons
                        CartActionButtons(
                            cartItems = cartItems,
                            onClearCart = { viewModel.clearCart() },
                            onHoldCartClick = {
                                if (cartItems.isEmpty()) {
                                    showHoldCartDialog = true
                                } else {
                                    viewModel.saveHoldCart("Guest Cart")
                                }
                            },
                            onPrint = {
                                viewModel.printLatestOrder()
                            })
                    }

                    // Payment Input and Keypad positioned at bottom
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        // Payment Input
                        PaymentInput(
                            paymentAmount = paymentAmount,
                            onPaymentAmountChange = { paymentAmount = it },
                            onRemoveDigit = {
                                val current =
                                    paymentAmount.replace("$", "").replace(",", "").toDoubleOrNull()
                                        ?: 0.0
                                val newAmount = viewModel.removeLastDigit(current)
                                paymentAmount = viewModel.formatAmount(newAmount)
                            })

                        Spacer(modifier = Modifier.height(8.dp))

                        // Keypad
                        Keypad(isCashSelected = viewModel.isCashSelected, onDigitClick = { digit ->
                            val current =
                                paymentAmount.replace("$", "").replace(",", "").toDoubleOrNull()
                                    ?: 0.0
                            val newAmount = viewModel.appendDigitToAmount(current, digit)
                            paymentAmount = viewModel.formatAmount(newAmount)
                        }, onAmountSet = { amount ->
                            paymentAmount = viewModel.formatAmount(amount)
                        }, onExactAmount = {
                            // Calculate card total using PricingSummaryUseCase to match the displayed value
                            val summaryResult =
                                viewModel.pricingSummaryUseCase.calculatePricingSummary(
                                    cartItems = cartItems,
                                    subtotal = subtotal,
                                    cashDiscountTotal = cashDiscountTotal,
                                    orderDiscountTotal = orderDiscountTotal,
                                    isCashSelected = viewModel.isCashSelected
                                )
                            paymentAmount = viewModel.formatAmount(summaryResult.cardTotal)
                        }, onCashSelected = {
                            // Set cash as selected payment method
                            viewModel.isCashSelected = true
                            viewModel.updateCartPrices()

                            // Validate and proceed with order creation
                            if (cartItems.isEmpty()) {
                                DialogHandler.showDialog(
                                    message = "Cart is empty. Please add items to cart before creating an order.",
                                    buttonText = "OK",
                                    iconRes = R.drawable.info_icon
                                )
                            } else {
                                // Validate payment amount
                                val currentPayment =
                                    paymentAmount.replace("$", "").replace(",", "").toDoubleOrNull()
                                        ?: 0.0

                                // Use small epsilon for floating point comparison
                                val epsilon = 0.01

                                // Use PricingSummaryUseCase to get the correct cash total (matching displayed value)
                                val summaryResult =
                                    viewModel.pricingSummaryUseCase.calculatePricingSummary(
                                        cartItems = cartItems,
                                        subtotal = subtotal,
                                        cashDiscountTotal = cashDiscountTotal,
                                        orderDiscountTotal = orderDiscountTotal,
                                        isCashSelected = true // Cash is selected at this point
                                    )
                                val cashTotalAmount = summaryResult.cashTotal

                                if (cashTotalAmount > 0 && currentPayment < 0.01) {
                                    // Switch to card when payment amount is zero
                                    viewModel.isCashSelected = false
                                    viewModel.updateCartPrices()
                                    // Show error message only, don't auto-fill
                                    DialogHandler.showDialog(
                                        message = "Payment amount cannot be zero. Please enter the payment amount.",
                                        buttonText = "OK",
                                        iconRes = R.drawable.info_icon
                                    )
                                } else if (cashTotalAmount > 0 && currentPayment < cashTotalAmount - epsilon) {
                                    // Show error if payment is less than total
                                    DialogHandler.showDialog(
                                        message = "Payment amount ($${
                                            viewModel.formatAmount(
                                                currentPayment
                                            )
                                        }) is less than total amount ($${
                                            viewModel.formatAmount(
                                                cashTotalAmount
                                            )
                                        }). Please enter the full amount.",
                                        buttonText = "OK",
                                        iconRes = R.drawable.info_icon
                                    )
                                } else {
                                    // Payment equals or exceeds total amount - proceed with cash order
                                    // Calculate amount tendered (change) if payment is greater than cash total
                                    amountTendered = if (currentPayment > cashTotalAmount) {
                                        currentPayment - cashTotalAmount
                                    } else {
                                        0.0 // Exact payment, no change
                                    }
                                    viewModel.createOrder("cash")
                                }
                            }
                        }, onCardSelected = {
                            // Set card as selected payment method
                            viewModel.isCashSelected = false
                            viewModel.updateCartPrices()

                            // Validate and proceed with order creation
                            if (cartItems.isEmpty()) {
                                DialogHandler.showDialog(
                                    message = "Cart is empty. Please add items to cart before creating an order.",
                                    buttonText = "OK",
                                    iconRes = R.drawable.info_icon
                                )
                            } else {
                                // Validate payment amount
                                val currentPayment =
                                    paymentAmount.replace("$", "").replace(",", "").toDoubleOrNull()
                                        ?: 0.0

                                // Use small epsilon for floating point comparison
                                val epsilon = 0.01

                                if (cardTotal > 0 && currentPayment < 0.01) {
                                    // Show error message only, don't auto-fill
                                    DialogHandler.showDialog(
                                        message = "Payment amount cannot be zero. Please enter the payment amount.",
                                        buttonText = "OK",
                                        iconRes = R.drawable.info_icon
                                    )
                                } else if (cardTotal > 0 && currentPayment < cardTotal - epsilon) {
                                    // Show error if payment is less than total
                                    DialogHandler.showDialog(
                                        message = "Payment amount ($${
                                            viewModel.formatAmount(
                                                currentPayment
                                            )
                                        }) is less than total amount ($${
                                            viewModel.formatAmount(
                                                cardTotal
                                            )
                                        }). Please enter the full amount.",
                                        buttonText = "OK",
                                        iconRes = R.drawable.info_icon
                                    )
                                } else {
                                    // Payment equals or exceeds total amount - proceed with card payment
                                    viewModel.initCardPayment()
                                }
                            }
                        }, onClear = {
                            paymentAmount = "0.00"
                        }, onNext = {
                            // Round off payment amount to nearest integer
                            val currentPayment =
                                paymentAmount.replace("$", "").replace(",", "").toDoubleOrNull()
                                    ?: 0.0
                            val roundedAmount = kotlin.math.round(currentPayment).toDouble()
                            paymentAmount = viewModel.formatAmount(roundedAmount)
                        })
                    }
                }

                // Column 3 - Categories (25% width, full height)
                CategoriesPanel(
                    modifier = Modifier.weight(0.12f),
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        selectedCategory = category
                        viewModel.loadProducts(category.id)
                    })

                // Column 4 - Products (25% width)
                ProductsPanel(
                    modifier = Modifier.weight(0.3f),
                    navController = navController,
                    products = if (searchQuery.isNotEmpty()) searchResults else products,
                    cartItems = cartItems,
                    viewModel = viewModel,
                    onShowOrderDiscountDialog = {
                        if (cartItems.isEmpty()) {
                            DialogHandler.showDialog("There are no items in cart")
                        } else if (!viewModel.canApplyOrderLevelDiscount()) {
                            DialogHandler.showDialog("You can't apply order level discount after applied cash discount. If you need to apply order level discount click on card first")
                        } else {
                            showOrderDiscountDialog = true
                        }
                    },
                    onShowAddCustomerDialog = {
                        showAddCustomerDialog = true
                    },
                    onShowPLUSearchDialog = {
                        showPLUSearchDialog = true
                    },
                    onProductClick = { product ->
                        val variants = product.variants
                        if (variants != null && variants.isNotEmpty()) {
                            // Show variant selection dialog
                            selectedProductForVariant = product
                        } else {
                            // Add directly to cart if no variants
                            val success = viewModel.addToCart(product)
                            if (!success) {
                                DialogHandler.showDialog("You can't add product after applying cash discount. If you want to add click on card first")
                            }
                        }
                    })
            }
        }

        // Order Level Discount Dialog
        if (showOrderDiscountDialog) {
            OrderLevelDiscountDialog(
                onDismiss = { showOrderDiscountDialog = false },
                onApplyDiscount = { discounts ->
                    viewModel.setOrderLevelDiscounts(discounts)
                    viewModel.updateCartPrices()
                    showOrderDiscountDialog = false
                },
                onSaveDiscountValues = { discountValue, discountType, discountReason ->
                    viewModel.saveOrderDiscountValues(
                        discountValue, discountType, discountReason
                    )
                },
                onRemoveAllDiscounts = {
                    viewModel.removeAllOrderDiscounts()
                },
                onResetDiscountValues = {
                    viewModel.resetOrderDiscountValues()
                },
                preFilledDiscountValue = viewModel.getOrderDiscountValue(),
                preFilledDiscountType = viewModel.getOrderDiscountType(),
                preFilledDiscountReason = viewModel.getOrderDiscountReason(),
                existingOrderDiscounts = orderLevelDiscounts,
                maxDiscountAmount = viewModel.getCurrentSubtotalAfterOrderDiscounts()
            )
        }

        // Add Customer Dialog
        if (showAddCustomerDialog) {
            AddCustomerDialog(
                onDismiss = { showAddCustomerDialog = false },
                onSaveCustomer = { firstName, lastName, email, phoneNumber, birthday ->
                    viewModel.saveCustomer(firstName, lastName, email, phoneNumber, birthday)
                    showAddCustomerDialog = false
                })
        }

        // Hold Cart List Dialog
        if (showPLUSearchDialog) {
            PLUSearchDialog(
                onDismiss = {
                    showPLUSearchDialog = false
                    viewModel.clearPLUSearchResult()
                },
                onProductSelected = { product ->
                    val success = viewModel.addToCart(product)
                    if (success) {
                        showPLUSearchDialog = false
                        viewModel.clearPLUSearchResult()
                    } else {
                        DialogHandler.showDialog(
                            message = "Cannot add product to cart. Cash discount may be applied.",
                            buttonText = "OK"
                        ) {}
                    }
                },
                pluSearchResult = pluSearchResult,
                isSearchingPLU = isSearchingPLU,
                onPLUChanged = { plu ->
                    if (plu.length == 4) {
                        viewModel.searchProductByPLU(plu)
                    }
                })
        }

        if (showHoldCartDialog) {
            HoldCartListDialog(
                onDismiss = { showHoldCartDialog = false },
                onRestoreCart = { holdCartId ->
                    viewModel.restoreHoldCart(holdCartId)
                    showHoldCartDialog = false
                },
                onDeleteCart = { holdCartId ->
                    viewModel.deleteHoldCart(holdCartId)
                })
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            LogoutConfirmationDialog(onDismiss = { showLogoutDialog = false }, onConfirm = {
                // Navigate to pinCode immediately when logout button is clicked
                navController.navigate("pinCode") {
                    popUpTo(0) { inclusive = true }
                }
            })
        }

        if (selectedProductForVariant != null) {
            VariantSelectionDialog(
                product = selectedProductForVariant!!,
                onDismiss = { selectedProductForVariant = null },
                onVariantSelected = { variant ->
                    // Add variant to cart
                    val success = viewModel.addVariantToCart(selectedProductForVariant!!, variant)
                    if (success) {
                        selectedProductForVariant = null
                    } else {
                        DialogHandler.showDialog("You can't add product after applying cash discount. If you want to add click on card first")
                        selectedProductForVariant = null
                    }
                })
        }

        // Payment Success Dialog
        if (showPaymentSuccessDialog) {
            Dialog(onDismissRequest = { }) {
                PaymentSuccessUI(amountTendered = paymentSuccessAmount, onPrint = {
                    viewModel.printLatestOrder()
                }, onDone = {
                    // Clear cart and reset payment after dialog is closed
                    viewModel.clearCart()
                    paymentAmount = "0.00"
                    showPaymentSuccessDialog = false
                })
            }
        }
    }
}

@Composable
fun CartPanel(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    holdCartCount: Int,
    onRemoveFromCart: (Int, Int?) -> Unit,
    onUpdateCartItem: (CartItem) -> Unit,
    onAddCustomer: () -> Unit,
    onHoldCartClick: () -> Unit,
    canApplyProductDiscount: () -> Boolean,
    canRemoveItemFromCart: () -> Boolean
) {
    var selectedCartItem by remember { mutableStateOf<CartItem?>(null) }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Order Header
            CartHeader(
                cartItemsCount = cartItems.size,
                holdCartItemsCount = holdCartCount,
                onHoldCartClick = onHoldCartClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items or Empty State
            if (cartItems.isEmpty()) {
                EmptyCartState()
            } else {
                CartItemsList(
                    cartItems = cartItems,
                    onRemoveFromCart = { cartItem ->
                        cartItem.productId?.let { productId ->
                            if (cartItem.productVariantId != null) {
                                onRemoveFromCart(productId, cartItem.productVariantId)
                            } else {
                                onRemoveFromCart(productId, null)
                            }
                        }
                    },
                    onUpdateCartItem = { cartItem ->
                        if (canApplyProductDiscount()) {
                            selectedCartItem = cartItem
                        } else {
                            DialogHandler.showDialog("You can't apply product level discount after applied cash discount. If you need to apply product level discount click on card first")
                        }
                    },
                    canApplyProductDiscount = canApplyProductDiscount,
                    canRemoveItemFromCart = canRemoveItemFromCart
                )
            }
        }

        // Product Level Discount Dialog
        selectedCartItem?.let { cartItem ->
            ProductLevelDiscountDialog(
                cartItem = cartItem,
                onDismiss = { selectedCartItem = null },
                onApplyDiscount = { updatedItem ->
                    onUpdateCartItem(updatedItem)
                    selectedCartItem = null
                },
                onRemoveItem = {
                    onRemoveFromCart(cartItem.productId ?: 0, cartItem.productVariantId)
                    selectedCartItem = null
                })
        }
    }
}

@Composable
fun CartActionButtons(
    cartItems: List<CartItem>,
    onClearCart: () -> Unit,
    onHoldCartClick: () -> Unit,
    onPrint: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Clear Cart
        Card(
            onClick = onClearCart,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.light_grey))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.clear_cart_btn),
                    contentDescription = "Clear Cart",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Hold Cart
        Card(
            onClick = onHoldCartClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.light_grey))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hold_cart_btn),
                    contentDescription = "Hold Cart",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Price Check
        Card(
            onClick = { showComingSoonDialog() },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.light_grey))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.price_check_btn),
                    contentDescription = "Price Check",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Print Last Receipt
        Card(
            onClick = onPrint,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.light_grey))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.print_last_receipt_btn),
                    contentDescription = "Print Last Receipt",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun PricingSummary(
    subtotal: Double,
    cashDiscountTotal: Double,
    orderDiscountTotal: Double,
    tax: Double,
    cashTotal: Double,
    cardTotal: Double,
    isCashSelected: Boolean,
    cartItems: List<CartItem>,
    pricingSummaryUseCase: PricingSummaryUseCase
) {
    // Calculate pricing summary using use case
    val summaryResult =
        remember(cartItems, subtotal, cashDiscountTotal, orderDiscountTotal, isCashSelected) {
            pricingSummaryUseCase.calculatePricingSummary(
                cartItems = cartItems,
                subtotal = subtotal,
                cashDiscountTotal = cashDiscountTotal,
                orderDiscountTotal = orderDiscountTotal,
                isCashSelected = isCashSelected
            )
        }

    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cash Price Card
        PriceCard(
            modifier = Modifier.weight(1f),
            title = "Cash Price",
            headerColor = colorResource(id = R.color.green_success),
            subtotal = summaryResult.cashSubtotal,
            discount = summaryResult.totalCashDiscount,
            tax = summaryResult.cashTax,
            total = summaryResult.cashTotal
        )

        // Card Price Card
        PriceCard(
            modifier = Modifier.weight(1f),
            title = "Card Price",
            headerColor = colorResource(id = R.color.primary),
            subtotal = summaryResult.cardSubtotal,
            discount = summaryResult.cardDiscount,
            tax = summaryResult.cardTax,
            total = summaryResult.cardTotal
        )
    }
}

@Composable
fun PriceCard(
    modifier: Modifier = Modifier,
    title: String,
    headerColor: Color,
    subtotal: Double,
    discount: Double,
    tax: Double,
    total: Double
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                BaseText(
                    text = title,
                    color = Color.White,
                    fontSize = 12f,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GeneralSans
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subtotal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Subtotal:",
                        color = Color.Black,
                        fontSize = 14f,
                        fontWeight = FontWeight.Medium,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = String.format(Locale.US, "$%.2f", subtotal),
                        color = Color.Black,
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Discount
                if (discount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Discount:",
                            color = colorResource(id = R.color.green_success),
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium
                        )
                        BaseText(
                            text = String.format(Locale.US, "-$%.2f", discount),
                            color = colorResource(id = R.color.green_success),
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Tax
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Tax:",
                        color = Color.Black,
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium
                    )
                    BaseText(
                        text = String.format(Locale.US, "$%.2f", tax),
                        color = Color.Black,
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Divider
                HorizontalDivider(
                    color = Color.Gray,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Total:",
                        color = colorResource(id = R.color.primary),
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold
                    )
                    BaseText(
                        text = String.format(Locale.US, "$%.2f", total),
                        color = colorResource(id = R.color.primary),
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}


@Composable
fun CartHeader(
    cartItemsCount: Int, holdCartItemsCount: Int = 0, onHoldCartClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorResource(id = R.color.primary), shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BaseText(
            text = "Cart Item: $cartItemsCount",
            color = Color.White,
            fontSize = 12f,
            fontFamily = GeneralSans
        )

        // Hold Cart section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onHoldCartClick() }) {
            BaseText(
                text = "Hold Cart", color = Color.White, fontSize = 12f, fontFamily = GeneralSans
            )
            Spacer(modifier = Modifier.width(4.dp))

            // Cart icon with counter badge
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cart_icon),
                    contentDescription = "Hold Cart",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )

                // Counter badge
                if (holdCartItemsCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = Color.Red, shape = CircleShape
                            ), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (holdCartItemsCount > 9) "9+" else holdCartItemsCount.toString(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCartState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cart_icon),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = colorResource(id = R.color.cart_screen_btn_clr)
            )

            Spacer(modifier = Modifier.height(8.dp))

            BaseText(
                text = stringResource(id = R.string.empty),
                color = Color.DarkGray,
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CartItemsList(
    cartItems: List<CartItem>,
    onRemoveFromCart: (CartItem) -> Unit,
    onUpdateCartItem: (CartItem) -> Unit,
    canApplyProductDiscount: () -> Boolean,
    canRemoveItemFromCart: () -> Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp, bottom = 5.dp, start = 8.dp, end = 8.dp),
        ) {
            items(
                items = cartItems,
                key = { item -> "${item.productId}_${item.productVariantId ?: "no_variant"}" }) { item ->
                CartItemRow(
                    item = item, onRemove = { onRemoveFromCart(item) }, onUpdate = onUpdateCartItem
                )
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem, onRemove: () -> Unit, onUpdate: (CartItem) -> Unit
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = with(density) { 100.dp.toPx() } // Reduced threshold for easier removal

    // Calculate unit price after discount
    val unitPriceAfterDiscount = item.getProductDiscountedPrice()

    // Calculate total price (unit price × quantity)
    val totalPrice = unitPriceAfterDiscount * item.quantity

    // Original unit price for comparison
    val originalUnitPrice = item.selectedPrice

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Delete background (red background that shows when swiping) - similar to ItemTouchHelper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.Red, shape = RoundedCornerShape(4.dp)
                )
                .padding(8.dp), contentAlignment = Alignment.CenterEnd
        ) {
            BaseText(
                text = "DELETE",
                color = Color.White,
                fontSize = 12f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Bold
            )
        }

        // Main content row - mimics ItemTouchHelper.LEFT behavior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .background(
                    color = Color.White, shape = RoundedCornerShape(4.dp)
                )
                .pointerInput(item.productId) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Similar to ItemTouchHelper onSwiped behavior
                            if (offsetX < -swipeThreshold) {
                                // Swipe threshold reached - remove item (like ItemTouchHelper.LEFT)
                                onRemove()
                            } else {
                                // Snap back to original position (like ItemTouchHelper animation)
                                offsetX = 0f
                            }
                        }) { _, dragAmount ->
                        // Only allow swiping to the left (ItemTouchHelper.LEFT direction)
                        // Allow more aggressive swiping for better detection
                        val newOffset =
                            (offsetX + dragAmount).coerceAtLeast(-swipeThreshold * 2f)
                                .coerceAtMost(0f)
                        offsetX = newOffset
                    }
                }
                .clickable { onUpdate(item) }
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            // Product Image - Left most
            if (!item.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder when no image is available
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray), contentAlignment = Alignment.Center
                ) {
                    BaseText(
                        text = "IMG", color = Color.Gray, fontSize = 10f, fontFamily = GeneralSans
                    )
                }
            }

            // Product Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                BaseText(
                    text = item.name!!,
                    color = Color.Black,
                    fontSize = 12f,
                    fontFamily = GeneralSans
                )

                BaseText(
                    text = "Qty: ${item.quantity}",
                    color = Color.Gray,
                    fontSize = 10f,
                    fontFamily = GeneralSans
                )
            }

            // Price Details
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (unitPriceAfterDiscount < originalUnitPrice) {
                    // Show discount - display original total and discounted total
                    val originalTotal = originalUnitPrice * item.quantity
                    BaseText(
                        text = "$${String.format("%,.2f", originalTotal)}",
                        color = Color.Gray,
                        fontSize = 10f,
                        textDecoration = TextDecoration.LineThrough
                    )
                    BaseText(
                        text = "$${String.format("%,.2f", totalPrice)}",
                        color = Color.Black,
                        fontSize = 12f,
                        fontFamily = GeneralSans
                    )
                } else {
                    // No discount - show total price
                    BaseText(
                        text = "$${String.format("%,.2f", totalPrice)}",
                        color = Color.Black,
                        fontSize = 12f,
                        fontFamily = GeneralSans
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentInput(
    paymentAmount: String, onPaymentAmountChange: (String) -> Unit, onRemoveDigit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        BaseText(
            text = stringResource(id = R.string.payment_amount),
            color = Color.Black,
            fontSize = 12f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White, shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.borderOutline),
                    shape = RoundedCornerShape(4.dp)
                )
                .height(40.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = paymentAmount, onValueChange = { newValue ->
                    // Prevent deletion below 0.00
                    val currentAmount =
                        paymentAmount.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    val newAmount =
                        newValue.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0

                    // Only allow changes if the new amount is not less than 0.00
                    if (newAmount >= 0.0) {
                        onPaymentAmountChange(newValue)
                    }
                }, modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        // Prevent keyboard from opening by consuming all pointer events
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }, textStyle = TextStyle(
                    fontFamily = GeneralSans, fontSize = 14.sp, color = Color.Black
                ), cursorBrush = SolidColor(Color.Transparent), // Hide cursor
                readOnly = true, // Prevent keyboard from opening
                decorationBox = { innerTextField ->
                    innerTextField()
                })

            IconButton(
                onClick = {
                    // Only allow removal if current amount is greater than 0.00
                    val currentAmount =
                        paymentAmount.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    if (currentAmount > 0.0) {
                        onRemoveDigit()
                    }
                },
                modifier = Modifier.size(24.dp),
                enabled = (paymentAmount.replace("$", "").replace(",", "").toDoubleOrNull()
                    ?: 0.0) > 0.0
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.clear_text_icon),
                    contentDescription = "Remove Digit",
                    modifier = Modifier.size(16.dp),
                    tint = if ((paymentAmount.replace("$", "").replace(",", "").toDoubleOrNull()
                            ?: 0.0) > 0.0
                    ) Color.Gray else Color.LightGray
                )
            }
        }
    }
}

@Composable
fun Keypad(
    isCashSelected: Boolean,
    onDigitClick: (String) -> Unit,
    onAmountSet: (Double) -> Unit,
    onExactAmount: () -> Unit,
    onCashSelected: () -> Unit,
    onCardSelected: () -> Unit,
    onClear: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Row 1: 7, 8, 9, $1, $5
        KeypadRow(
            isCashSelected = isCashSelected,
            buttons = listOf("7", "8", "9", "$1", "$5"),
            onDigitClick = onDigitClick,
            onAmountSet = onAmountSet,
            onCashSelected = onCashSelected,
            onCardSelected = onCardSelected,
            onClear = onClear,
            onNext = onNext
        )

        // Row 2: 4, 5, 6, $10, $20
        KeypadRow(
            isCashSelected = isCashSelected,
            buttons = listOf("4", "5", "6", "$10", "$20"),
            onDigitClick = onDigitClick,
            onAmountSet = onAmountSet,
            onCashSelected = onCashSelected,
            onCardSelected = onCardSelected,
            onClear = onClear,
            onNext = onNext
        )

        // Row 3: 1, 2, 3, $50, $100
        KeypadRow(
            isCashSelected = isCashSelected,
            buttons = listOf("1", "2", "3", "$50", "$100"),
            onDigitClick = onDigitClick,
            onAmountSet = onAmountSet,
            onCashSelected = onCashSelected,
            onCardSelected = onCardSelected,
            onClear = onClear,
            onNext = onNext
        )

        // Row 4: Exact, 0, Next, Cash
        KeypadRow(
            isCashSelected = isCashSelected,
            buttons = listOf(
                stringResource(id = R.string.exact),
                stringResource(id = R.string._0),
                stringResource(id = R.string.next),
                stringResource(id = R.string.cash)
            ),
            onDigitClick = onDigitClick,
            onAmountSet = onAmountSet,
            onExactAmount = onExactAmount,
            onCashSelected = onCashSelected,
            onCardSelected = onCardSelected,
            onClear = onClear,
            onNext = onNext,
            isRow4 = true
        )

        // Row 5: Empty, 00, Clear, Card
        KeypadRow(
            isCashSelected = isCashSelected,
            buttons = listOf(
                "",
                stringResource(id = R.string._00),
                stringResource(id = R.string.clear),
                stringResource(id = R.string.card)
            ),
            onDigitClick = onDigitClick,
            onAmountSet = onAmountSet,
            onCashSelected = onCashSelected,
            onCardSelected = onCardSelected,
            onClear = onClear,
            onNext = onNext,
            isRow5 = true
        )
    }
}

@Composable
fun KeypadRow(
    isCashSelected: Boolean,
    buttons: List<String>,
    onDigitClick: (String) -> Unit,
    onAmountSet: (Double) -> Unit,
    onExactAmount: (() -> Unit)? = null,
    onCashSelected: (() -> Unit)? = null,
    onCardSelected: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    isLastRow: Boolean = false,
    isRow4: Boolean = false,
    isRow5: Boolean = false
) {
    val strExact = stringResource(id = R.string.exact)
    val cash = stringResource(id = R.string.cash)
    val card = stringResource(id = R.string.card)
    val clear = stringResource(id = R.string.clear)
    val next = stringResource(id = R.string.next)

    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        buttons.forEachIndexed { index, button ->
            // Calculate weight for Row 4: first 3 buttons (Exact, 0, Next) = 0.9f, last button (Cash) = 2.2f
            // Calculate weight for Row 5: first button (Empty) = 0.9f (matches Exact position), 00 and Clear = 0.9f (matches Next/0), Card button = 2.3f (matches Cash width)
            val buttonWeight = when {
                button.contains("$") -> 1.2f
                isRow4 && index == buttons.size - 1 && button == stringResource(id = R.string.cash) -> 2.2f
                isRow4 && index < buttons.size - 1 -> 0.9f // First 3 buttons in Row 4 (Exact, 0, Next)
                isRow5 && index == buttons.size - 1 && button == stringResource(id = R.string.card) -> 2.2f // Card button - slightly larger than Cash to match visually
                isRow5 && index == 0 && button.isEmpty() -> 0.9f // Empty button in Row 5 - aligns with Exact button position
                isRow5 && (button == stringResource(id = R.string._00) || button == stringResource(
                    id = R.string.clear
                )) -> 0.9f // 00 and Clear buttons - same size as Next/0
                isLastRow && button == stringResource(id = R.string.cash) -> 2.2f
                else -> 1f // Default weight for regular buttons (like 1, 2, 3 in Row 3)
            }

            KeypadButton(
                text = button,
                modifier = Modifier.weight(buttonWeight),
                onClick = {
                    when {
                        button.contains("$") -> {
                            val amount = button.replace("$", "").toDoubleOrNull() ?: 0.0
                            onAmountSet(amount)
                        }

                        button == strExact -> {
                            onExactAmount?.invoke()
                        }

                        button == cash -> {
                            onCashSelected?.invoke()
                        }

                        button == card -> {
                            onCardSelected?.invoke()
                        }

                        button == clear -> {
                            onClear?.invoke()
                        }

                        button == next -> {
                            onNext?.invoke()
                        }

                        button.isNotEmpty() -> {
                            onDigitClick(button)
                        }
                    }
                },
                isActionButton = button.contains("$") || button == stringResource(id = R.string.exact) || button == stringResource(
                    id = R.string.next
                ) || button == stringResource(id = R.string.clear),
                isPaymentButton = button == stringResource(id = R.string.cash) || button == stringResource(
                    id = R.string.card
                ),
                isCashSelected = isCashSelected
            )
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isActionButton: Boolean = false,
    isPaymentButton: Boolean = false,
    isCashSelected: Boolean = false
) {
    if (text.isEmpty()) {
        Spacer(modifier = modifier.height(36.dp))
        return
    }

    val backgroundColor = when {
        isPaymentButton -> {
            val cash = stringResource(id = R.string.cash)
            val card = stringResource(id = R.string.card)
            when {
                text == cash && isCashSelected -> colorResource(id = R.color.green_success) // Cash selected = green
                text == cash && !isCashSelected -> colorResource(id = R.color.primary) // Cash not selected = primary
                text == card && !isCashSelected -> colorResource(id = R.color.green_success) // Card selected (default) = green
                text == card && isCashSelected -> colorResource(id = R.color.primary) // Card not selected = primary
                else -> colorResource(id = R.color.primary)
            }
        }

        isActionButton -> colorResource(id = R.color.primary)
        else -> colorResource(id = R.color.pricing_calculator_clr)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
    ) {
        BaseText(
            text = text,
            color = Color.White,
            fontSize = 16f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PaymentMethods(
    onCashSelected: () -> Unit, onCardSelected: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Button(
            onClick = onCashSelected,
            modifier = Modifier
                .weight(2f)
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.primary)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cash_icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                BaseText(
                    text = stringResource(id = R.string.cash),
                    color = Color.White,
                    fontSize = 12f,
                    fontFamily = GeneralSans
                )
            }
        }

        Button(
            onClick = onCardSelected,
            modifier = Modifier
                .weight(2f)
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.green_success)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.card_icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                BaseText(
                    text = stringResource(id = R.string.card),
                    color = Color.White,
                    fontSize = 12f,
                    fontFamily = GeneralSans
                )
            }
        }
    }
}

@Composable
fun CategoriesPanel(
    modifier: Modifier = Modifier,
    categories: List<CategoryData>,
    selectedCategory: CategoryData?,
    onCategorySelected: (CategoryData) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .background(colorResource(id = R.color.light_grey))
            .padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(categories) { category ->
            CategoryItem(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                onClick = { onCategorySelected(category) })
        }
    }
}

@Composable
fun CategoryItem(
    category: CategoryData, isSelected: Boolean, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorResource(id = R.color.primary) else Color.White
        ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            BaseText(
                text = category.title,
                color = if (isSelected) Color.White else Color.Black,
                fontSize = 13f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProductsPanel(
    modifier: Modifier = Modifier,
    navController: NavController,
    products: List<Products>,
    cartItems: List<CartItem>,
    viewModel: HomeViewModel,
    onProductClick: (Products) -> Unit,
    onShowOrderDiscountDialog: () -> Unit,
    onShowAddCustomerDialog: () -> Unit,
    onShowPLUSearchDialog: () -> Unit
) {
    val bottomNavMenus by viewModel.menus.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine current selected index based on current destination
    val selectedIndex = remember(currentDestination) {
        val route = currentDestination?.route
        when (route) {
            "home" -> 0
            "products" -> 1
            "orders" -> 2
            "inventory" -> 3
            "reports" -> 4
            "setup" -> 5
            else -> 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colorResource(id = R.color.light_grey))
            .padding(8.dp)
    ) {
        // Products Grid - Takes available space
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products) { product ->
                ProductItem(
                    product = product, onClick = { onProductClick(product) })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons - Fixed above navigation rows
        ActionButtonsPanel(
            modifier = Modifier.fillMaxWidth(),
            navController = navController,
            cartItems = cartItems,
            onShowOrderDiscountDialog = onShowOrderDiscountDialog,
            onShowAddCustomerDialog = onShowAddCustomerDialog,
            onShowPLUSearchDialog = onShowPLUSearchDialog,
            viewModel = viewModel
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Buttons - Fixed at bottom (2 rows)
        Column(
            modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1 - First 4 navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                bottomNavMenus.take(4).forEachIndexed { index, menu ->
                    NavigationButton(
                        menu = menu, isSelected = selectedIndex == index, onClick = {
                            // Handle cash drawer button - just call function, don't navigate
                            if (menu.destinationId == R.id.cashDrawerScreen) {
                                viewModel.openCashDrawer("Manual Open")
                            } else {
                                // Map resource IDs to navigation routes
                                val route = when (menu.destinationId) {
                                    R.id.homeScreen -> "home"
                                    R.id.productsScreen -> "products"
                                    R.id.ordersScreen -> "orders"
                                    R.id.inventoryScreen -> "inventory"
                                    R.id.reportsScreen -> "reports"
                                    R.id.setupScreen -> "setup"
                                    else -> null
                                }
                                route?.let {
                                    navController.navigate(it) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }, modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 2 - Remaining 4 navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                bottomNavMenus.drop(4).forEachIndexed { index, menu ->
                    NavigationButton(
                        menu = menu, isSelected = selectedIndex == (index + 4), onClick = {
                            // Handle cash drawer button - just call function, don't navigate
                            if (menu.destinationId == R.id.cashDrawerScreen) {
                                viewModel.openCashDrawer("Manual Open")
                            } else {
                                // Map resource IDs to navigation routes
                                val route = when (menu.destinationId) {
                                    R.id.homeScreen -> "home"
                                    R.id.productsScreen -> "products"
                                    R.id.ordersScreen -> "orders"
                                    R.id.inventoryScreen -> "inventory"
                                    R.id.reportsScreen -> "reports"
                                    R.id.setupScreen -> "setup"
                                    else -> null
                                }
                                route?.let {
                                    navController.navigate(it) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Products, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Column 1 - Product Image
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (product.images!!.isNotEmpty()) {
                    AsyncImage(
                        model = product.images!!.first().fileURL,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Column 2 - Product Name and Weight
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // Row 1 - Product Name
                BaseText(
                    text = product.name!!,
                    color = Color.Black,
                    fontSize = 10f,
                    fontFamily = GeneralSans,
                    textAlign = TextAlign.Left,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2 - Product Weight (placeholder - update when weight field is available)
                BaseText(
                    text = product.quantity.toString() + " units", // Placeholder - replace with weight when available
                    color = Color.Gray,
                    fontSize = 9f,
                    fontFamily = GeneralSans,
                    textAlign = TextAlign.Left,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ActionButtonsPanel(
    modifier: Modifier = Modifier,
    navController: NavController,
    cartItems: List<CartItem>,
    onShowOrderDiscountDialog: () -> Unit,
    onShowAddCustomerDialog: () -> Unit,
    onShowPLUSearchDialog: () -> Unit,
    viewModel: HomeViewModel
) {
    val isTaxExempt by viewModel.isTaxExempt.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
    ) {
        // Row 1
        ActionButtonRow(
            rowIndex = 0, buttons = listOf(
                ActionButton("EBT"),
                ActionButton("Split"),
                ActionButton("Customer"),
                ActionButton("Discount"),
            ), onActionClick = { action ->
                when (action) {
                    "Discount" -> {
                        onShowOrderDiscountDialog()
                    }

                    "Customer" -> {
                        onShowAddCustomerDialog()
                    }

                    else -> {
                        showComingSoonDialog()
                    }
                }
            })

        Spacer(modifier = Modifier.height(4.dp))

        // Row 2
        ActionButtonRow(
            rowIndex = 1, buttons = listOf(
                ActionButton("Custom Sales"),
                ActionButton("PLU Search"),
                ActionButton("Pay In/Out"),
                ActionButton("Refund"),
            ), onActionClick = { action ->
                when (action) {
                    "PLU Search" -> {
                        onShowPLUSearchDialog()
                    }

                    else -> {
                        showComingSoonDialog()
                    }
                }
            })

        Spacer(modifier = Modifier.height(4.dp))

        // Row 3
        ActionButtonRow(
            rowIndex = 2, buttons = listOf(
                ActionButton("Weight Scale"), ActionButton("Rewards"), ActionButton(
                    if (isTaxExempt) "Apply Tax" else "Tax-Exempt"
                ), ActionButton("Void")

            ), onActionClick = { action ->
                when (action) {
                    "Tax-Exempt", "Apply Tax" -> {
                        viewModel.toggleTaxExempt()
                    }

                    else -> {
                        showComingSoonDialog()
                    }
                }
            })
    }
}

data class ActionButton(
    val label: String
)

@Composable
fun ActionButtonRow(
    rowIndex: Int = 0,
    modifier: Modifier = Modifier,
    buttons: List<ActionButton>,
    onActionClick: (String) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(0.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        buttons.forEachIndexed { buttonIndex, button ->
            // Use custom red color background for Discount, Refund, Void, and Tax-Exempt buttons, default blue for others
            val backgroundColor =
                if (button.label == "Discount" || button.label == "Refund" || button.label == "Void" || button.label == "Tax-Exempt" || button.label == "Apply Tax") {
                    Color(0xFFDC3E42)
                } else {
                    Color(0xFF043E7F)
                }

            // Text color - white for all buttons
            val textColor = Color.White

            // Apply slightly rounded corners to all buttons
            val cornerRadius = RoundedCornerShape(4.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .background(
                        color = backgroundColor, shape = cornerRadius
                    )
                    .clickable { onActionClick(button.label) }, contentAlignment = Alignment.Center
            ) {
                BaseText(
                    text = button.label,
                    color = textColor,
                    fontSize = 12f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NavigationButton(
    menu: BottomMenu, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        colorResource(id = R.color.primary)
    } else {
        colorResource(id = R.color.nav_bar_button_clr)
    }

    val textColor = if (isSelected) {
        Color.White
    } else {
        Color.Black
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor, contentColor = textColor
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        BaseText(
            text = menu.menuName,
            fontSize = 12f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun ProductLevelDiscountDialog(
    cartItem: CartItem,
    onDismiss: () -> Unit,
    onApplyDiscount: (CartItem) -> Unit,
    onRemoveItem: () -> Unit
) {
    val context = LocalContext.current
    var quantity by remember { mutableStateOf(cartItem.quantity) }
    var discountValue by remember {
        mutableStateOf(
            if (cartItem.discountValue != null && cartItem.discountValue != 0.0) {
                cartItem.discountValue.toString()
            } else {
                ""
            }
        )
    }
    var discountReason by remember { mutableStateOf(cartItem.discountReason ?: "") }
    var discountType by remember { mutableStateOf(cartItem.discountType) }
    // UI toggle is 'Tax Exempt' – store inverse in chargeTaxOnThisProduct
    var isTaxExempt by remember { mutableStateOf(cartItem.chargeTaxOnThisProduct == false) }

    AlertDialog(onDismissRequest = onDismiss, title = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BaseText(
                text = "Product Discount",
                color = Color.Black,
                fontSize = 16f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                BaseText(
                    text = "✕",
                    color = Color.Black,
                    fontSize = 18f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }, text = {
        Column(
            modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Name
            BaseText(
                text = cartItem.name!!,
                color = Color.Black,
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )

            // Quantity Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BaseText(
                    text = "Quantity:",
                    color = Color.Black,
                    fontSize = 12f,
                    fontFamily = GeneralSans
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- }) {
                        BaseText(
                            text = "-",
                            color = Color.Black,
                            fontSize = 18f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    BaseText(
                        text = quantity.toString(),
                        color = Color.Black,
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(
                        onClick = { quantity++ }) {
                        BaseText(
                            text = "+",
                            color = Color.Black,
                            fontSize = 18f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Price
            BaseText(
                text = "Price: $${String.format("%,.2f", cartItem.selectedPrice)}",
                color = Color.Black,
                fontSize = 12f,
                fontFamily = GeneralSans
            )

            // Tax Switch (Tax Exempt)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BaseText(
                    text = "Tax Exempt",
                    color = Color.Black,
                    fontSize = 12f,
                    fontFamily = GeneralSans
                )
                Switch(
                    checked = isTaxExempt,
                    onCheckedChange = { isTaxExempt = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colorResource(id = R.color.primary),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray
                    )
                )
            }

            // Discount Section
            BaseText(
                text = "Discount",
                color = Color.Black,
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )

            // Discount Value
            BasicTextField(
                value = discountValue,
                onValueChange = { discountValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                textStyle = TextStyle(
                    fontFamily = GeneralSans, fontSize = 12.sp, color = Color.Black
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    innerTextField()
                })

            // Discount Type Radio Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = discountType == com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE,
                        onClick = {
                            discountType =
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorResource(id = R.color.primary),
                            unselectedColor = Color.Gray
                        )
                    )
                    BaseText(
                        text = "Percentage",
                        color = Color.Black,
                        fontSize = 12f,
                        fontFamily = GeneralSans
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = discountType == com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT,
                        onClick = {
                            discountType =
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorResource(id = R.color.primary),
                            unselectedColor = Color.Gray
                        )
                    )
                    BaseText(
                        text = "Amount",
                        color = Color.Black,
                        fontSize = 12f,
                        fontFamily = GeneralSans
                    )
                }
            }

            // Discount Reason
            BasicTextField(
                value = discountReason,
                onValueChange = { discountReason = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = GeneralSans, fontSize = 12.sp, color = Color.Black
                ),
                decorationBox = { innerTextField ->
                    if (discountReason.isEmpty()) {
                        BaseText(
                            text = "Discount Reason",
                            color = Color.Gray,
                            fontSize = 12f,
                            fontFamily = GeneralSans
                        )
                    }
                    innerTextField()
                })
        }
    }, confirmButton = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onRemoveItem, colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                )
            ) {
                BaseText(
                    text = "Remove", color = Color.Red, fontSize = 12f, fontFamily = GeneralSans
                )
            }
            Button(
                onClick = {
                    val discountValueDouble = discountValue.toDoubleOrNull() ?: 0.0

                    // Validations (matching your Fragment logic exactly)
                    when {
                        quantity <= 0 -> {
                            DialogHandler.showDialog("Quantity must be at least 1")
                            return@Button
                        }
                        // If discount value entered but no type selected
                        discountValueDouble > 0 && discountType == null -> {
                            DialogHandler.showDialog("Select discount type")
                            return@Button
                        }
                        // If percentage discount, must be <= 100
                        discountType == com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE && discountValueDouble > 100 -> {
                            DialogHandler.showDialog("Percentage cannot be more than 100")
                            return@Button
                        }
                        // If amount discount, must not exceed product price
                        discountType == com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT && discountValueDouble > cartItem.selectedPrice -> {
                            DialogHandler.showDialog("Discount amount cannot be more than product price")
                            return@Button
                        }
                        // If discount applied, reason must not be empty
                        discountValueDouble > 0 && discountReason.isEmpty() -> {
                            DialogHandler.showDialog("Please enter discount reason")
                            return@Button
                        }
                    }

                    val updatedCartItem = cartItem.copy(
                        quantity = quantity,
                        // chargeTaxOnThisProduct = NOT tax exempt
                        chargeTaxOnThisProduct = !isTaxExempt,
                        discountType = discountType,
                        discountValue = discountValueDouble,
                        discountReason = discountReason
                    )
                    onApplyDiscount(updatedCartItem)
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary)
                )
            ) {
                BaseText(
                    text = "Apply",
                    color = Color.White,
                    fontSize = 12f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderLevelDiscountDialog(
    onDismiss: () -> Unit,
    onApplyDiscount: (List<com.retail.dolphinpos.domain.model.home.order_discount.OrderDiscount>) -> Unit,
    onSaveDiscountValues: (String, String, String) -> Unit,
    onRemoveAllDiscounts: () -> Unit,
    onResetDiscountValues: () -> Unit,
    preFilledDiscountValue: String = "",
    preFilledDiscountType: String = "PERCENTAGE",
    preFilledDiscountReason: String = "Select Reason",
    existingOrderDiscounts: List<com.retail.dolphinpos.domain.model.home.order_discount.OrderDiscount> = emptyList(),
    maxDiscountAmount: Double = 0.0
) {
    val context = LocalContext.current
    var discountValue by remember { mutableStateOf(preFilledDiscountValue) }
    var discountType by remember {
        mutableStateOf(
            if (preFilledDiscountType == "PERCENTAGE") {
                com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE
            } else {
                com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT
            }
        )
    }
    var selectedReason by remember { mutableStateOf(preFilledDiscountReason) }
    var orderDiscounts by remember { mutableStateOf(existingOrderDiscounts) }

    // Calculate available discount amount considering discounts already added in dialog
    val availableDiscountAmount = remember(orderDiscounts, maxDiscountAmount) {
        var available = maxDiscountAmount
        for (discount in orderDiscounts) {
            available = when (discount.type) {
                com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE -> {
                    available - (available * discount.value / 100.0)
                }

                com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT -> {
                    available - discount.value
                }

                else -> available
            }
            if (available < 0) available = 0.0
        }
        available.coerceAtLeast(0.0)
    }

    val reasons = listOf(
        "Select Reason",
        "Customer Loyalty",
        "Bulk Purchase",
        "Seasonal Sale",
        "First Time Customer",
        "Employee Discount",
        "Manager Override",
        "Other"
    )

    AlertDialog(onDismissRequest = onDismiss, title = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BaseText(
                text = "Discount",
                color = Color.Black,
                fontSize = 16f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                BaseText(
                    text = "✕",
                    color = Color.Black,
                    fontSize = 18f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }, text = {
        Column(
            modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reason Selection
            BaseText(
                text = "Reason for Discount:",
                color = Color.Black,
                fontSize = 12f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )

            // Dropdown for reason selection
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedReason,
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    ),
                    textStyle = TextStyle(
                        fontSize = 12.sp, fontFamily = GeneralSans, color = Color.Black
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded, onDismissRequest = { expanded = false }) {
                    reasons.drop(1).forEach { reason ->
                        DropdownMenuItem(text = {
                            BaseText(
                                text = reason,
                                color = Color.Black,
                                fontSize = 12f,
                                fontFamily = GeneralSans
                            )
                        }, onClick = {
                            selectedReason = reason
                            expanded = false
                        })
                    }
                }
            }

            // Discount Value
            BaseText(
                text = "Discount Value:",
                color = Color.Black,
                fontSize = 12f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )

            BasicTextField(
                value = discountValue,
                onValueChange = { discountValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = GeneralSans, fontSize = 12.sp, color = Color.Black
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    innerTextField()
                })

            // Discount Type Radio Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = discountType == com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE,
                        onClick = {
                            discountType =
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorResource(id = R.color.primary),
                            unselectedColor = Color.Gray
                        )
                    )
                    BaseText(
                        text = "Percentage",
                        color = Color.Black,
                        fontSize = 12f,
                        fontFamily = GeneralSans
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = discountType == com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT,
                        onClick = {
                            discountType =
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorResource(id = R.color.primary),
                            unselectedColor = Color.Gray
                        )
                    )
                    BaseText(
                        text = "Amount",
                        color = Color.Black,
                        fontSize = 12f,
                        fontFamily = GeneralSans
                    )
                }
            }

            // Applied Discounts List
            if (orderDiscounts.isNotEmpty()) {
                BaseText(
                    text = "Applied Discounts:",
                    color = Color.Black,
                    fontSize = 12f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    modifier = Modifier.height(60.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orderDiscounts) { discount ->
                        Card(
                            modifier = Modifier.width(120.dp), colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            ), shape = RoundedCornerShape(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                BaseText(
                                    text = discount.reason,
                                    color = Color.Black,
                                    fontSize = 11f,
                                    fontFamily = GeneralSans
                                )
                                BaseText(
                                    text = "${discount.value}${if (discount.type == com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE) "%" else "$"}",
                                    color = Color.Gray,
                                    fontSize = 10f,
                                    fontFamily = GeneralSans
                                )
                            }
                            IconButton(
                                onClick = {
                                    orderDiscounts = orderDiscounts.filter { it != discount }
                                }) {
                                BaseText(
                                    text = "✕",
                                    color = Color.Red,
                                    fontSize = 12f,
                                    fontFamily = GeneralSans,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }, confirmButton = {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Remove All Discounts button (only show if there are existing discounts)
                if (existingOrderDiscounts.isNotEmpty()) {
                    Button(
                        onClick = {
                            orderDiscounts = emptyList()
                            onRemoveAllDiscounts()
                            onResetDiscountValues()
                            onDismiss()
                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ), modifier = Modifier.weight(1f)
                    ) {
                        BaseText(
                            text = "Remove All",
                            color = Color.White,
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = {
                        val value = discountValue.toDoubleOrNull()
                        if (selectedReason != "Select Reason" && value != null && value > 0) {
                            // Validate discount amount based on type
                            val isValidDiscount = when (discountType) {
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT -> {
                                    // For amount type, check if discount doesn't exceed availableDiscountAmount
                                    if (value > availableDiscountAmount) {
                                        DialogHandler.showDialog(
                                            "Discount amount cannot exceed $${
                                                String.format(
                                                    "%.2f", availableDiscountAmount
                                                )
                                            }"
                                        )
                                        false
                                    } else {
                                        true
                                    }
                                }

                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE -> {
                                    // For percentage type, check if it doesn't exceed 100%
                                    if (value > 100) {
                                        DialogHandler.showDialog("Discount percentage cannot exceed 100%")
                                        false
                                    } else {
                                        true
                                    }
                                }

                                else -> true
                            }

                            if (isValidDiscount) {
                                val newDiscount =
                                    com.retail.dolphinpos.domain.model.home.order_discount.OrderDiscount(
                                        reason = selectedReason, type = discountType, value = value
                                    )
                                orderDiscounts = orderDiscounts + newDiscount

                                // Save current values for persistence
                                onSaveDiscountValues(
                                    discountValue,
                                    if (discountType == com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE) "PERCENTAGE" else "AMOUNT",
                                    selectedReason
                                )

                                // Reset fields
                                discountValue = ""
                                selectedReason = "Select Reason"
                                discountType =
                                    com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE
                            }
                        } else {
                            DialogHandler.showDialog("Please select reason and enter value")
                        }
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary)
                    ), modifier = Modifier.weight(1f)
                ) {
                    BaseText(
                        text = "Add Discount",
                        color = Color.White,
                        fontSize = 12f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        onApplyDiscount(orderDiscounts)
                        onDismiss()
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary)
                    ), modifier = Modifier.weight(1f)
                ) {
                    BaseText(
                        text = "Apply All",
                        color = Color.White,
                        fontSize = 12f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit, onSaveCustomer: (String, String, String, String, String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("Select Birthday") }
    var showDatePicker by remember { mutableStateOf(false) }
    var firstNameError by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title with Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BaseText(
                        text = "Add Customer",
                        color = Color.Black,
                        fontSize = 16f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_icon),
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // First Name and Last Name Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // First Name
                    Column(modifier = Modifier.weight(1f)) {
                        BaseText(
                            text = "First Name",
                            color = colorResource(id = R.color.primary),
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = {
                                firstName = it
                                firstNameError = ""
                            },
                            placeholder = {
                                BaseText(
                                    text = "Enter First Name",
                                    fontSize = 12f,
                                    fontFamily = GeneralSans
                                )
                            },
                            isError = firstNameError.isNotEmpty(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(id = R.color.primary),
                                unfocusedBorderColor = Color.Gray
                            ),
                            textStyle = TextStyle(
                                fontSize = 12.sp, fontFamily = GeneralSans, lineHeight = 14.sp
                            )
                        )
                        if (firstNameError.isNotEmpty()) {
                            BaseText(
                                text = firstNameError,
                                color = Color.Red,
                                fontSize = 10f,
                                fontFamily = GeneralSans
                            )
                        }
                    }

                    // Last Name
                    Column(modifier = Modifier.weight(1f)) {
                        BaseText(
                            text = "Last Name",
                            color = colorResource(id = R.color.primary),
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = {
                                lastName = it
                                lastNameError = ""
                            },
                            placeholder = {
                                BaseText(
                                    text = "Enter Last Name",
                                    fontSize = 12f,
                                    fontFamily = GeneralSans
                                )
                            },
                            isError = lastNameError.isNotEmpty(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(id = R.color.primary),
                                unfocusedBorderColor = Color.Gray
                            ),
                            textStyle = TextStyle(
                                fontSize = 12.sp, fontFamily = GeneralSans, lineHeight = 14.sp
                            )
                        )
                        if (lastNameError.isNotEmpty()) {
                            BaseText(
                                text = lastNameError,
                                color = Color.Red,
                                fontSize = 10f,
                                fontFamily = GeneralSans
                            )
                        }
                    }
                }

                // Email
                Column(modifier = Modifier.fillMaxWidth()) {
                    BaseText(
                        text = "Email",
                        color = colorResource(id = R.color.primary),
                        fontSize = 12f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = ""
                        },
                        placeholder = {
                            BaseText(
                                text = "Enter Email", fontSize = 12f, fontFamily = GeneralSans
                            )
                        },
                        isError = emailError.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorResource(id = R.color.primary),
                            unfocusedBorderColor = Color.Gray
                        ),
                        textStyle = TextStyle(
                            fontSize = 12.sp, fontFamily = GeneralSans, lineHeight = 14.sp
                        )
                    )
                    if (emailError.isNotEmpty()) {
                        BaseText(
                            text = emailError,
                            color = Color.Red,
                            fontSize = 10f,
                            fontFamily = GeneralSans
                        )
                    }
                }

                // Mobile Number and Birthday Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mobile Number
                    Column(modifier = Modifier.weight(1f)) {
                        BaseText(
                            text = "Mobile Number",
                            color = colorResource(id = R.color.primary),
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                phoneNumber = it
                            },
                            placeholder = {
                                BaseText(
                                    text = "Enter Mobile Number",
                                    fontSize = 12f,
                                    fontFamily = GeneralSans
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(id = R.color.primary),
                                unfocusedBorderColor = Color.Gray
                            ),
                            textStyle = TextStyle(
                                fontSize = 12.sp, fontFamily = GeneralSans, lineHeight = 14.sp
                            )
                        )
                    }

                    // Birthday
                    Column(modifier = Modifier.weight(1f)) {
                        BaseText(
                            text = "Birthday",
                            color = colorResource(id = R.color.primary),
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { showDatePicker = true }
                                .border(
                                    width = 1.dp,
                                    color = Color.Gray,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BaseText(
                                    text = birthday,
                                    fontSize = 13f,
                                    fontFamily = GeneralSans,
                                    color = if (birthday == "Select Birthday") Color.Gray else Color.Black
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.dropdown_icon),
                                    contentDescription = "Dropdown",
                                    modifier = Modifier.size(15.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add Button
                Button(
                    onClick = {
                        // Validation
                        var hasError = false

                        if (firstName.trim().isEmpty()) {
                            firstNameError = "First name is required"
                            hasError = true
                        }

                        if (lastName.trim().isEmpty()) {
                            lastNameError = "Last name is required"
                            hasError = true
                        }

                        if (email.trim().isEmpty()) {
                            emailError = "Email is required"
                            hasError = true
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            emailError = "Invalid email address"
                            hasError = true
                        }

                        if (!hasError) {
                            onSaveCustomer(
                                firstName.trim(),
                                lastName.trim(),
                                email.trim(),
                                phoneNumber.trim(),
                                if (birthday == "Select Birthday") "" else birthday
                            )
                        }
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary)
                    ), modifier = Modifier.align(Alignment.End)
                ) {
                    BaseText(
                        text = "Add",
                        color = Color.White,
                        fontSize = 12f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = today, initialDisplayedMonthMillis = today
        )

        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        // Only allow dates up to today
                        if (dateMillis <= today) {
                            val selectedCalendar = Calendar.getInstance()
                            selectedCalendar.timeInMillis = dateMillis
                            val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)
                            val month = selectedCalendar.get(Calendar.MONTH) + 1
                            val year = selectedCalendar.get(Calendar.YEAR)
                            birthday = "$day/$month/$year"
                            showDatePicker = false
                        }
                    }
                }) {
                Text("OK")
            }
        }, dismissButton = {
            TextButton(onClick = { showDatePicker = false }) {
                Text("Cancel")
            }
        }) {
            DatePicker(
                state = datePickerState, colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = colorResource(id = R.color.primary),
                    todayDateBorderColor = colorResource(id = R.color.primary)
                )
            )
        }
    }
}

@Composable
fun VariantSelectionDialog(
    product: Products, onDismiss: () -> Unit, onVariantSelected: (Variant) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(
            text = product.name ?: "Select Variant",
            fontFamily = GeneralSans,
            fontWeight = FontWeight.SemiBold
        )
    }, text = {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(product.variants.orEmpty()) { variant ->
                Card(
                    onClick = { onVariantSelected(variant) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Variant image (shown first)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center
                        ) {
                            if (variant.images.isNotEmpty() && variant.images.first().fileURL != null && variant.images.first().fileURL!!.isNotEmpty()) {
                                AsyncImage(
                                    model = variant.images.first().fileURL,
                                    contentDescription = variant.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "Product Placeholder",
                                    modifier = Modifier.size(40.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = variant.title ?: "Variant",
                                fontFamily = GeneralSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Price: $${variant.cardPrice?.toDoubleOrNull() ?: 0.0}",
                                fontFamily = GeneralSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Qty: ${variant.quantity}",
                                fontFamily = GeneralSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel", fontFamily = GeneralSans)
        }
    })
}

@Composable
fun PaymentSuccessUI(
    amountTendered: String, onPrint: () -> Unit, onDone: () -> Unit
) {
    // Format amount to ensure no minus sign for zero or negative values
    val formattedAmount = remember(amountTendered) {
        val amount = amountTendered.toDoubleOrNull() ?: 0.0
        // Ensure amount is at least 0 to avoid showing minus sign
        String.format(Locale.US, "%.2f", maxOf(0.0, amount))
    }
    Card(
        shape = RoundedCornerShape(5.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .width(500.dp)
            .height(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Green Checkmark Icon
            Icon(
                painter = painterResource(id = R.drawable.success_circle_icon),
                contentDescription = "Success",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Successful Text
            BaseText(
                text = "Payment Successful",
                fontSize = 20f,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                fontFamily = GeneralSans
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Tendered
            BaseText(
                text = "Amount Tender: $$formattedAmount",
                fontSize = 16f,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                fontFamily = GeneralSans
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Print Button
                OutlinedButton(
                    onClick = onPrint,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    BaseText(
                        text = "Print",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                }

                // Done Button
                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    BaseText(
                        text = "Done",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontFamily = GeneralSans
                    )
                }
            }
        }
    }
}

@Composable
fun PLUSearchDialog(
    onDismiss: () -> Unit,
    onProductSelected: (Products) -> Unit,
    pluSearchResult: Products?,
    isSearchingPLU: Boolean,
    onPLUChanged: (String) -> Unit
) {
    var pluInput by remember { mutableStateOf("") }
    val pluFocusRequester = remember { FocusRequester() }

    // Reset input when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            pluInput = ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxSize(0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BaseText(
                        text = "PLU Search",
                        fontSize = 20f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_icon),
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PLU Input Field
                OutlinedTextField(
                    value = pluInput,
                    onValueChange = { newValue ->
                        // Only allow numeric input and limit to 4 digits
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            pluInput = newValue
                            onPLUChanged(newValue)
                        }
                    },
                    label = {
                        BaseText(
                            text = "Enter 4-digit PLU",
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            color = colorResource(id = R.color.primary)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(pluFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(id = R.color.primary),
                        unfocusedBorderColor = Color.Gray
                    ),
                    textStyle = TextStyle(
                        fontSize = 18.sp, fontFamily = GeneralSans, fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Indicator or Result
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isSearchingPLU -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = colorResource(id = R.color.primary)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                BaseText(
                                    text = "Searching...",
                                    fontSize = 16f,
                                    fontFamily = GeneralSans,
                                    color = Color.Gray
                                )
                            }
                        }

                        pluSearchResult != null -> {
                            // Show product result
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Product Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onProductSelected(pluSearchResult)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Product Image
                                        val productImages = pluSearchResult.images
                                        if (productImages != null && productImages.isNotEmpty()) {
                                            AsyncImage(
                                                model = productImages[0].fileURL,
                                                contentDescription = pluSearchResult.name,
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .background(
                                                        Color.LightGray, RoundedCornerShape(8.dp)
                                                    ), contentAlignment = Alignment.Center
                                            ) {
                                                BaseText(
                                                    text = "No Image",
                                                    fontSize = 12f,
                                                    fontFamily = GeneralSans,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        // Product Details
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            BaseText(
                                                text = pluSearchResult.name ?: "Unknown Product",
                                                fontSize = 16f,
                                                fontFamily = GeneralSans,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            BaseText(
                                                text = "PLU: $pluInput",
                                                fontSize = 14f,
                                                fontFamily = GeneralSans,
                                                color = Color.Gray
                                            )
                                            BaseText(
                                                text = "$${
                                                    String.format(
                                                        "%.2f",
                                                        pluSearchResult.cardPrice.toDoubleOrNull() ?: 0.0
                                                    )
                                                }",
                                                fontSize = 18f,
                                                fontFamily = GeneralSans,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorResource(id = R.color.primary)
                                            )
                                        }
                                    }
                                }

                                // Instruction Text
                                BaseText(
                                    text = "Click on the product to add it to cart",
                                    fontSize = 14f,
                                    fontFamily = GeneralSans,
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        pluInput.length == 4 && !isSearchingPLU -> {
                            BaseText(
                                text = "No product found for PLU: $pluInput",
                                fontSize = 16f,
                                fontFamily = GeneralSans,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }

                        else -> {
                            BaseText(
                                text = "Enter a 4-digit PLU number",
                                fontSize = 16f,
                                fontFamily = GeneralSans,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    BaseText(
                        text = "Close",
                        fontSize = 16f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Auto-focus PLU input when dialog opens
    LaunchedEffect(Unit) {
        pluFocusRequester.requestFocus()
    }
}
