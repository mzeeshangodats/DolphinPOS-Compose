package com.retail.dolphinpos.presentation.features.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import java.util.Calendar
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HomeAppBar
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import com.retail.dolphinpos.common.utils.PreferenceManager
import java.util.Locale
import kotlin.math.roundToInt

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
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val subtotal by viewModel.subtotal.collectAsStateWithLifecycle()
    val tax by viewModel.tax.collectAsStateWithLifecycle()
    val totalAmount by viewModel.totalAmount.collectAsStateWithLifecycle()
    val cashDiscountTotal by viewModel.cashDiscountTotal.collectAsStateWithLifecycle()
    val orderDiscountTotal by viewModel.orderDiscountTotal.collectAsStateWithLifecycle()
    val orderLevelDiscounts by viewModel.orderLevelDiscounts.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchProductResults.collectAsStateWithLifecycle()
    val holdCartCount by viewModel.holdCartCount.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf<CategoryData?>(null) }
    var paymentAmount by remember { mutableStateOf("0.00") }
    var searchQuery by remember { mutableStateOf("") }
    
    // Get username and clock-in status from preferences
    val userName = preferenceManager.getName()
    val isClockedIn = preferenceManager.isClockedIn()
    val clockInTime = preferenceManager.getClockInTime()

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.homeUiEvent.collect { event ->
            when (event) {
                is HomeUiEvent.ShowLoading -> Loader.show("Loading...")
                is HomeUiEvent.HideLoading -> Loader.hide()
                is HomeUiEvent.ShowError -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = "OK"
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

    // Update payment amount when total changes
    LaunchedEffect(totalAmount) {
        paymentAmount = viewModel.formatAmount(totalAmount)
    }

    // Load hold carts when screen loads
    LaunchedEffect(Unit) {
        viewModel.loadHoldCarts()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Home App Bar with search and logout
            HomeAppBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    searchQuery = query
                    viewModel.searchProducts(query)
                },
                onLogout = {
                    // TODO: Implement logout functionality
                },
                searchResults = searchResults,
                onProductClick = { product ->
                    viewModel.addToCart(product)
                    searchQuery = ""
                },
                userName = userName,
                isClockedIn = isClockedIn,
                clockInTime = clockInTime
            )

            Row(
                modifier = Modifier.weight(1f)
            ) {
                // Column 1 - Cart (25% width, full height)
                CartPanel(
                    modifier = Modifier.weight(0.3f),
                    cartItems = cartItems,
                    holdCartCount = holdCartCount,
                    onRemoveFromCart = { productId ->
                        val success = viewModel.removeFromCart(productId)
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
                    canRemoveItemFromCart = { viewModel.canRemoveItemFromCart() }
                )

                // Column 2 - Pricing/Payment/Keypad + Action Buttons (25% width, full height)
                Column(
                    modifier = Modifier
                        .weight(0.25f)
                        .background(colorResource(id = R.color.light_grey))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pricing Summary
                    PricingSummary(
                        subtotal = subtotal,
                        cashDiscountTotal = cashDiscountTotal,
                        orderDiscountTotal = orderDiscountTotal,
                        tax = tax,
                        totalAmount = totalAmount,
                        isCashSelected = viewModel.isCashSelected
                    )

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
                        }
                    )

                    // Payment Input
                    PaymentInput(
                        paymentAmount = paymentAmount,
                        onPaymentAmountChange = { paymentAmount = it },
                        onRemoveDigit = {
                            val current = paymentAmount.replace("$", "").toDoubleOrNull() ?: 0.0
                            val newAmount = viewModel.removeLastDigit(current)
                            paymentAmount = viewModel.formatAmount(newAmount)
                        }
                    )

                    // Keypad
                    Keypad(
                        onDigitClick = { digit ->
                            val current = paymentAmount.replace("$", "").toDoubleOrNull() ?: 0.0
                            val newAmount = viewModel.appendDigitToAmount(current, digit)
                            paymentAmount = viewModel.formatAmount(newAmount)
                        },
                        onAmountSet = { amount ->
                            paymentAmount = viewModel.formatAmount(amount)
                        },
                        onExactAmount = {
                            paymentAmount = viewModel.formatAmount(totalAmount)
                        },
                        onCashSelected = {
                            viewModel.isCashSelected = true
                            viewModel.updateCartPrices()
                        },
                        onCardSelected = {
                            viewModel.isCashSelected = false
                            viewModel.updateCartPrices()
                        },
                        onClear = {
                            paymentAmount = "0.00"
                        }
                    )


                }

                // Column 3 - Categories (25% width, full height)
                CategoriesPanel(
                    modifier = Modifier.weight(0.15f),
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        selectedCategory = category
                        viewModel.loadProducts(category.id)
                    }
                )

                // Column 4 - Products (25% width)
                ProductsPanel(
                    modifier = Modifier.weight(0.3f),
                    products = if (searchQuery.isNotEmpty()) searchResults else products,
                    cartItems = cartItems,
                    onProductClick = { product ->
                        viewModel.addToCart(product)
                    },
                    onShowOrderDiscountDialog = {
                        if (cartItems.isEmpty()) {
                            DialogHandler.showDialog("There are no items in cart")
                        } else if (!viewModel.canApplyOrderLevelDiscount()) {
                            DialogHandler.showDialog("You can't apply order level discount after applied cash discount. If you need to apply order level discount click on card first")
                        } else {
                            showOrderDiscountDialog = true
                        }
                    }
                )
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
                            discountValue,
                            discountType,
                            discountReason
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
                    existingOrderDiscounts = orderLevelDiscounts
                )
            }

            // Add Customer Dialog
            if (showAddCustomerDialog) {
                AddCustomerDialog(
                    onDismiss = { showAddCustomerDialog = false },
                    onSaveCustomer = { firstName, lastName, email, birthday ->
                        viewModel.saveCustomer(firstName, lastName, email, birthday)
                        showAddCustomerDialog = false
                        DialogHandler.showDialog(
                            message = "Customer Added Successfully",
                            buttonText = "OK",
                            iconRes = R.drawable.add_customer_icon_blue,
                            cancellable = true
                        )
                    }
                )
            }

            // Hold Cart List Dialog
            if (showHoldCartDialog) {
                HoldCartListDialog(
                    onDismiss = { showHoldCartDialog = false },
                    onRestoreCart = { holdCartId ->
                        viewModel.restoreHoldCart(holdCartId)
                        showHoldCartDialog = false
                    },
                    onDeleteCart = { holdCartId ->
                        viewModel.deleteHoldCart(holdCartId)
                    }
                )
            }
        }
    }
}

@Composable
fun CartPanel(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    holdCartCount: Int,
    onRemoveFromCart: (Int) -> Unit,
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
                onAddCustomer = onAddCustomer,
                onHoldCartClick = onHoldCartClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items or Empty State
            if (cartItems.isEmpty()) {
                EmptyCartState()
            } else {
                CartItemsList(
                    cartItems = cartItems,
                    onRemoveFromCart = onRemoveFromCart,
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
                    onRemoveFromCart(cartItem.productId ?: 0)
                    selectedCartItem = null
                }
            )
        }
    }
}

@Composable
fun CartActionButtons(
    cartItems: List<CartItem>,
    onClearCart: () -> Unit,
    onHoldCartClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
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
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
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
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
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
            onClick = { /* TODO */ },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.light_grey))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
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
            onClick = { /* TODO */ },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.light_grey))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
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
    totalAmount: Double,
    isCashSelected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
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
                fontSize = 12f,
                fontFamily = GeneralSans
            )
            BaseText(
                text = String.format(Locale.US, "$%.2f", subtotal),
                color = Color.Black,
                fontSize = 12f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Cash Discount
        if (isCashSelected && cashDiscountTotal > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BaseText(
                    text = "Cash Discount:",
                    color = colorResource(id = R.color.green_success),
                    fontSize = 12f,
                    fontFamily = GeneralSans
                )
                BaseText(
                    text = String.format(Locale.US, "-$%.2f", cashDiscountTotal),
                    color = colorResource(id = R.color.green_success),
                    fontSize = 12f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Order Discount
        if (orderDiscountTotal > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BaseText(
                    text = "Discount:",
                    color = colorResource(id = R.color.green_success),
                    fontSize = 12f,
                    fontFamily = GeneralSans
                )
                BaseText(
                    text = String.format(Locale.US, "-$%.2f", orderDiscountTotal),
                    color = colorResource(id = R.color.green_success),
                    fontSize = 12f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.SemiBold
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
                fontSize = 12f,
                fontFamily = GeneralSans
            )
            BaseText(
                text = String.format(Locale.US, "$%.2f", tax),
                color = Color.Black,
                fontSize = 12f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Divider
        HorizontalDivider(
            color = Color.Gray,
            thickness = 1.dp
        )

        // Total Amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BaseText(
                text = "Total:",
                color = colorResource(id = R.color.primary),
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Bold
            )
            BaseText(
                text = String.format(Locale.US, "$%.2f", totalAmount),
                color = colorResource(id = R.color.primary),
                fontSize = 14f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun CartHeader(
    cartItemsCount: Int,
    holdCartItemsCount: Int = 0,
    onAddCustomer: () -> Unit,
    onHoldCartClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorResource(id = R.color.primary),
                shape = RoundedCornerShape(4.dp)
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
            modifier = Modifier.clickable { onHoldCartClick() }
        ) {
            BaseText(
                text = "Hold Cart",
                color = Color.White,
                fontSize = 12f,
                fontFamily = GeneralSans
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
                                color = Color.Red,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onAddCustomer() }
        ) {
            BaseText(
                text = "Non Member",
                color = Color.White,
                fontSize = 12f,
                fontFamily = GeneralSans
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(id = R.drawable.add_customer_icon),
                contentDescription = "Add Customer",
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun EmptyCartState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(4.dp)
            )
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

@Composable
fun CartItemsList(
    cartItems: List<CartItem>,
    onRemoveFromCart: (Int) -> Unit,
    onUpdateCartItem: (CartItem) -> Unit,
    canApplyProductDiscount: () -> Boolean,
    canRemoveItemFromCart: () -> Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(top = 5.dp, bottom = 5.dp, start = 8.dp, end = 8.dp),
    ) {
        items(
            items = cartItems,
            key = { item -> item.productId ?: 0 }
        ) { item ->
            CartItemRow(
                item = item,
                onRemove = { item.productId?.let { id -> onRemoveFromCart(id) } },
                onUpdate = onUpdateCartItem
            )
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit,
    onUpdate: (CartItem) -> Unit
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = with(density) { 100.dp.toPx() } // Reduced threshold for easier removal

    val finalPrice = run {
        val discount = when (item.discountType) {
            com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE ->
                (item.selectedPrice * (item.discountValue ?: 0.0) / 100.0)

            com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT ->
                item.discountValue ?: 0.0

            else -> 0.0
        }
        (item.selectedPrice - discount).coerceAtLeast(0.0)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Delete background (red background that shows when swiping) - similar to ItemTouchHelper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.Red,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.CenterEnd
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
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp)
                )
                .pointerInput(item.productId) {
                    detectDragGestures(
                        onDragEnd = {
                            // Similar to ItemTouchHelper onSwiped behavior
                            if (offsetX < -swipeThreshold) {
                                // Swipe threshold reached - remove item (like ItemTouchHelper.LEFT)
                                onRemove()
                            } else {
                                // Snap back to original position (like ItemTouchHelper animation)
                                offsetX = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        // Only allow swiping to the left (ItemTouchHelper.LEFT direction)
                        // Allow more aggressive swiping for better detection
                        val newOffset = (offsetX + dragAmount.x).coerceAtLeast(-swipeThreshold * 2f)
                            .coerceAtMost(0f)
                        offsetX = newOffset
                    }
                }
                .clickable { onUpdate(item) }
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    BaseText(
                        text = "IMG",
                        color = Color.Gray,
                        fontSize = 10f,
                        fontFamily = GeneralSans
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
                if (finalPrice < item.selectedPrice) {
                    // Show discount
                    BaseText(
                        text = "$${String.format("%.2f", item.selectedPrice)}",
                        color = Color.Gray,
                        fontSize = 10f,
                        textDecoration = TextDecoration.LineThrough
                    )
                    BaseText(
                        text = "$${String.format("%.2f", finalPrice)}",
                        color = Color.Black,
                        fontSize = 12f,
                        fontFamily = GeneralSans
                    )
                } else {
                    BaseText(
                        text = "$${String.format("%.2f", finalPrice)}",
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
    paymentAmount: String,
    onPaymentAmountChange: (String) -> Unit,
    onRemoveDigit: () -> Unit
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
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.borderOutline),
                    shape = RoundedCornerShape(4.dp)
                )
                .height(35.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = paymentAmount,
                onValueChange = { newValue ->
                    // Prevent deletion below 0.00
                    val currentAmount = paymentAmount.replace("$", "").toDoubleOrNull() ?: 0.0
                    val newAmount = newValue.replace("$", "").toDoubleOrNull() ?: 0.0

                    // Only allow changes if the new amount is not less than 0.00
                    if (newAmount >= 0.0) {
                        onPaymentAmountChange(newValue)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        // Prevent keyboard from opening by consuming all pointer events
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    },
                textStyle = TextStyle(
                    fontFamily = GeneralSans,
                    fontSize = 12.sp,
                    color = Color.Black
                ),
                cursorBrush = SolidColor(Color.Transparent), // Hide cursor
                readOnly = true, // Prevent keyboard from opening
                decorationBox = { innerTextField ->
                    innerTextField()
                }
            )

            IconButton(
                onClick = {
                    // Only allow removal if current amount is greater than 0.00
                    val currentAmount = paymentAmount.replace("$", "").toDoubleOrNull() ?: 0.0
                    if (currentAmount > 0.0) {
                        onRemoveDigit()
                    }
                },
                modifier = Modifier.size(24.dp),
                enabled = (paymentAmount.replace("$", "").toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.clear_text_icon),
                    contentDescription = "Remove Digit",
                    modifier = Modifier.size(16.dp),
                    tint = if ((paymentAmount.replace("$", "").toDoubleOrNull()
                            ?: 0.0) > 0.0
                    ) Color.Gray else Color.LightGray
                )
            }
        }
    }
}

@Composable
fun Keypad(
    onDigitClick: (String) -> Unit,
    onAmountSet: (Double) -> Unit,
    onExactAmount: () -> Unit,
    onCashSelected: () -> Unit,
    onCardSelected: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Row 1: 7, 8, 9, $1, $5
        KeypadRow(
            buttons = listOf("7", "8", "9", "$1", "$5"),
            onDigitClick = onDigitClick,
            onAmountSet = onAmountSet,
            onCashSelected = onCashSelected,
            onCardSelected = onCardSelected,
            onClear = onClear
        )

        // Row 2: 4, 5, 6, $10, $20
        KeypadRow(
            buttons = listOf("4", "5", "6", "$10", "$20"),
            onDigitClick = onDigitClick,
            onAmountSet = onAmountSet,
            onCashSelected = onCashSelected,
            onCardSelected = onCardSelected,
            onClear = onClear
        )

        // Row 3: 1, 2, 3, $50, $100
        KeypadRow(
            buttons = listOf("1", "2", "3", "$50", "$100"),
            onDigitClick = onDigitClick,
            onAmountSet = onAmountSet,
            onCashSelected = onCashSelected,
            onCardSelected = onCardSelected,
            onClear = onClear
        )

        // Row 4: Exact, 0, Next, Cash
        KeypadRow(
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
            onClear = onClear
        )

        // Row 5: Empty, 00, Clear, Card
        KeypadRow(
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
            onClear = onClear
        )
    }
}

@Composable
fun KeypadRow(
    buttons: List<String>,
    onDigitClick: (String) -> Unit,
    onAmountSet: (Double) -> Unit,
    onExactAmount: (() -> Unit)? = null,
    onCashSelected: (() -> Unit)? = null,
    onCardSelected: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    isLastRow: Boolean = false
) {
    val strExact = stringResource(id = R.string.exact)
    val cash = stringResource(id = R.string.cash)
    val card = stringResource(id = R.string.card)
    val clear = stringResource(id = R.string.clear)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        buttons.forEach { button ->
            KeypadButton(
                text = button,
                modifier = Modifier.weight(
                    if (button.contains("$")) 1.2f else if (isLastRow && button == stringResource(
                            id = R.string.cash
                        )
                    ) 2f else 1f
                ),
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

                        button.isNotEmpty() -> {
                            onDigitClick(button)
                        }
                    }
                },
                isActionButton = button.contains("$") || button == stringResource(id = R.string.exact) ||
                        button == stringResource(id = R.string.next) || button == stringResource(id = R.string.clear),
                isPaymentButton = button == stringResource(id = R.string.cash) || button == stringResource(
                    id = R.string.card
                )
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
    isPaymentButton: Boolean = false
) {
    if (text.isEmpty()) {
        Spacer(modifier = modifier.height(36.dp))
        return
    }

    val backgroundColor = when {
        isPaymentButton -> colorResource(id = if (text == stringResource(id = R.string.cash)) R.color.primary else R.color.green_success)
        isActionButton -> colorResource(id = R.color.primary)
        else -> colorResource(id = R.color.pricing_calculator_clr)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
    ) {
        BaseText(
            text = text,
            color = Color.White,
            fontSize = 13f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PaymentMethods(
    onCashSelected: () -> Unit,
    onCardSelected: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(categories) { category ->
            CategoryItem(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: CategoryData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) colorResource(id = R.color.primary) else Color.White
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        BaseText(
            text = category.title,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 12f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProductsPanel(
    modifier: Modifier = Modifier,
    products: List<Products>,
    cartItems: List<CartItem>,
    onProductClick: (Products) -> Unit,
    onShowOrderDiscountDialog: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colorResource(id = R.color.light_grey))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Products Grid - 60% of height
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(0.59f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products) { product ->
                ProductItem(
                    product = product,
                    onClick = { onProductClick(product) }
                )
            }
        }

        // Action Buttons - 40% of height
        ActionButtonsPanel(
            modifier = Modifier.weight(0.41f),
            cartItems = cartItems,
            onShowOrderDiscountDialog = onShowOrderDiscountDialog
        )
    }
}

@Composable
fun ProductItem(
    product: Products,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (product.images!!.isNotEmpty()) {
                AsyncImage(
                    model = product.images!!.first().fileURL,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            BaseText(
                text = product.name!!,
                color = Color.Black,
                fontSize = 10f,
                fontFamily = GeneralSans,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActionButtonsPanel(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    onShowOrderDiscountDialog: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(5.dp),
    ) {
        // Row 1
        ActionButtonRow(
            buttons = listOf(
                ActionButton("Split", R.drawable.split_btn),
                ActionButton("Gift Card", R.drawable.gift_card_btn),
                ActionButton("Pending Orders", R.drawable.pending_orders_button),
                ActionButton("Refund", R.drawable.refund_btn)
            ),
            onActionClick = { action ->
                // TODO: Add action handlers for these buttons
            }
        )

        // Row 2
        ActionButtonRow(
            buttons = listOf(
                ActionButton("EBT", R.drawable.ebt_btn),
                ActionButton("Rewards", R.drawable.rewards_btn),
                ActionButton("Online Order", R.drawable.online_order_btn),
                ActionButton("Tax Exempt", R.drawable.tax_exempt_btn)
            ),
            onActionClick = { action ->
                // TODO: Add action handlers for these buttons
            }
        )

        // Row 3
        ActionButtonRow(
            buttons = listOf(
                ActionButton("Custom Sales", R.drawable.custom_sales),
                ActionButton("Last Receipt", R.drawable.last_receipt),
                ActionButton("Pay In/Out", R.drawable.pay_in_out_btn),
                ActionButton("Void", R.drawable.void_btn)
            ),
            onActionClick = { action ->
                // TODO: Add action handlers for these buttons
            }
        )

        // Row 4
        ActionButtonRow(
            buttons = listOf(
                ActionButton("Promotions", R.drawable.promotions_btn),
                ActionButton("Weight Scale", R.drawable.weight_scale_btn),
                ActionButton("Clock In/Out", R.drawable.clock_in_out_btn),
                ActionButton("Order Discount", R.drawable.discount_btn)
            ),
            onActionClick = { action ->
                when (action) {
                    "Order Discount" -> {
                        onShowOrderDiscountDialog()
                    }
                    // TODO: Add other action handlers
                }
            }
        )
    }
}

data class ActionButton(
    val label: String,
    val iconRes: Int
)

@Composable
fun ActionButtonRow(
    buttons: List<ActionButton>,
    onActionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        buttons.forEach { button ->
            Card(
                onClick = { onActionClick(button.label) },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.light_grey))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = button.iconRes),
                        contentDescription = button.label,
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
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
    var chargeTax by remember { mutableStateOf(cartItem.chargeTaxOnThisProduct) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            onClick = { if (quantity > 1) quantity-- }
                        ) {
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
                            onClick = { quantity++ }
                        ) {
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
                    text = "Price: $${String.format("%.2f", cartItem.selectedPrice)}",
                    color = Color.Black,
                    fontSize = 12f,
                    fontFamily = GeneralSans
                )

                // Tax Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BaseText(
                        text = "Charge Tax",
                        color = Color.Black,
                        fontSize = 12f,
                        fontFamily = GeneralSans
                    )
                    Switch(
                        checked = chargeTax!!,
                        onCheckedChange = { chargeTax = it },
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
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    textStyle = TextStyle(
                        fontFamily = GeneralSans,
                        fontSize = 12.sp,
                        color = Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        innerTextField()
                    }
                )

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
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = GeneralSans,
                        fontSize = 12.sp,
                        color = Color.Black
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
                    }
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onRemoveItem,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    BaseText(
                        text = "Remove",
                        color = Color.Red,
                        fontSize = 12f,
                        fontFamily = GeneralSans
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
                            // If discount applied, reason must not be empty
                            discountValueDouble > 0 && discountReason.isEmpty() -> {
                                DialogHandler.showDialog("Please enter discount reason")
                                return@Button
                            }
                        }

                        val updatedCartItem = cartItem.copy(
                            quantity = quantity,
                            chargeTaxOnThisProduct = chargeTax,
                            discountType = discountType,
                            discountValue = discountValueDouble,
                            discountReason = discountReason
                        )
                        onApplyDiscount(updatedCartItem)
                    },
                    colors = ButtonDefaults.buttonColors(
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
        }
    )
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
    existingOrderDiscounts: List<com.retail.dolphinpos.domain.model.home.order_discount.OrderDiscount> = emptyList()
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
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
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        textStyle = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = GeneralSans,
                            color = Color.Black
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        reasons.drop(1).forEach { reason ->
                            DropdownMenuItem(
                                text = {
                                    BaseText(
                                        text = reason,
                                        color = Color.Black,
                                        fontSize = 12f,
                                        fontFamily = GeneralSans
                                    )
                                },
                                onClick = {
                                    selectedReason = reason
                                    expanded = false
                                }
                            )
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
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = GeneralSans,
                        fontSize = 12.sp,
                        color = Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        innerTextField()
                    }
                )

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
                                modifier = Modifier.width(120.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                shape = RoundedCornerShape(4.dp)
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
                                    }
                                ) {
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
        },
        confirmButton = {
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
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            ),
                            modifier = Modifier.weight(1f)
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
                                val newDiscount =
                                    com.retail.dolphinpos.domain.model.home.order_discount.OrderDiscount(
                                        reason = selectedReason,
                                        type = discountType,
                                        value = value
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
                            } else {
                                DialogHandler.showDialog("Please select reason and enter value")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C757D)
                        ),
                        modifier = Modifier.weight(1f)
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
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary)
                        ),
                        modifier = Modifier.weight(1f)
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
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSaveCustomer: (String, String, String, String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
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
                // Title
                BaseText(
                    text = "Add Customer",
                    color = Color.Black,
                    fontSize = 16f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Medium
                )

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
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = {
                                firstName = it
                                firstNameError = ""
                            },
                            placeholder = {
                                BaseText(
                                    text = "Enter First Name",
                                    fontSize = 13f,
                                    fontFamily = GeneralSans
                                )
                            },
                            isError = firstNameError.isNotEmpty(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(id = R.color.primary),
                                unfocusedBorderColor = Color.Gray
                            ),
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                fontFamily = GeneralSans
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
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = {
                                lastName = it
                                lastNameError = ""
                            },
                            placeholder = {
                                BaseText(
                                    text = "Enter Last Name",
                                    fontSize = 13f,
                                    fontFamily = GeneralSans
                                )
                            },
                            isError = lastNameError.isNotEmpty(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(id = R.color.primary),
                                unfocusedBorderColor = Color.Gray
                            ),
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                fontFamily = GeneralSans
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

                // Email and Birthday Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Email
                    Column(modifier = Modifier.weight(1f)) {
                        BaseText(
                            text = "Email",
                            color = colorResource(id = R.color.primary),
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = ""
                            },
                            placeholder = {
                                BaseText(
                                    text = "Enter Email",
                                    fontSize = 13f,
                                    fontFamily = GeneralSans
                                )
                            },
                            isError = emailError.isNotEmpty(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(id = R.color.primary),
                                unfocusedBorderColor = Color.Gray
                            ),
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                fontFamily = GeneralSans
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

                    // Birthday
                    Column(modifier = Modifier.weight(1f)) {
                        BaseText(
                            text = "Birthday",
                            color = colorResource(id = R.color.primary),
                            fontSize = 12f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { showDatePicker = true }
                                .border(
                                    width = 1.dp,
                                    color = Color.Gray,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
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
                                if (birthday == "Select Birthday") "" else birthday
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary)
                    ),
                    modifier = Modifier.align(Alignment.End)
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
            initialSelectedDateMillis = today,
            initialDisplayedMonthMillis = today
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
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
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = colorResource(id = R.color.primary),
                    todayDateBorderColor = colorResource(id = R.color.primary)
                )
            )
        }
    }
}
