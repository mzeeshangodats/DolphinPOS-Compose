package com.retail.dolphinpos.presentation.features.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.fragment.app.FragmentActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBar
import com.retail.dolphinpos.common.components.LogoutConfirmationDialog
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailList
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun OrdersScreen(
    navController: NavController,
    viewModel: OrdersViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedOrder by remember { mutableStateOf<OrderDetailList?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val displayDateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val apiDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dismiss = stringResource(id = R.string.dismiss)
    val context = LocalContext.current

    // Get activity for fragment manager
    val activity = remember {
        when {
            context is FragmentActivity -> context
            context is android.app.Activity -> {
                try {
                    context as? FragmentActivity
                } catch (e: Exception) {
                    null
                }
            }

            else -> null
        }
    }

    // State to trigger date pickers
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Get username and clock-in status from preferences
    val userName = preferenceManager.getName()
    val isClockedIn = preferenceManager.isClockedIn()
    val clockInTime = preferenceManager.getClockInTime()

    // Filter orders based on search query
    val filteredOrders = if (searchQuery.isEmpty()) {
        orders
    } else {
        orders.filter {
            it.orderNumber.contains(searchQuery, ignoreCase = true) ||
                    it.paymentMethod.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            // Debounce search - reload after user stops typing
            kotlinx.coroutines.delay(500)
            viewModel.loadOrders(searchQuery)
        } else {
            viewModel.loadOrders()
        }
    }

    // Handle start date picker
    LaunchedEffect(showStartDatePicker) {
        if (showStartDatePicker && activity != null) {
            try {
                val constraints = CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now())
                    .build()

                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Start Date")
                    .setCalendarConstraints(constraints)
                    .build()

                datePicker.addOnPositiveButtonClickListener { selectedDate ->
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calendar.timeInMillis = selectedDate
                    val dateStr = apiDateFormat.format(calendar.time)

                    // Validate: Start date should not be after end date
                    val currentEndDate = endDate
                    if (currentEndDate != null) {
                        try {
                            val selectedDateParsed = apiDateFormat.parse(dateStr)
                            val endDateParsed = apiDateFormat.parse(currentEndDate)
                            if (selectedDateParsed != null && endDateParsed != null && selectedDateParsed.after(
                                    endDateParsed
                                )
                            ) {
                                // Show error dialog
                                DialogHandler.showDialog(
                                    message = "Start date cannot be selected after end date",
                                    buttonText = dismiss
                                ) {}
                                return@addOnPositiveButtonClickListener
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "OrdersScreen",
                                "Error validating dates: ${e.message}"
                            )
                        }
                    }

                    viewModel.setStartDate(dateStr)
                }

                datePicker.addOnDismissListener {
                    showStartDatePicker = false
                }

                datePicker.show(activity.supportFragmentManager, "StartDatePicker")
            } catch (e: Exception) {
                android.util.Log.e("OrdersScreen", "Error showing start date picker: ${e.message}")
                showStartDatePicker = false
            }
        } else if (showStartDatePicker && activity == null) {
            android.util.Log.e(
                "OrdersScreen",
                "Activity is not FragmentActivity, cannot show date picker"
            )
            showStartDatePicker = false
        }
    }

    // Handle end date picker
    LaunchedEffect(showEndDatePicker) {
        if (showEndDatePicker && activity != null) {
            try {
                val constraints = CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now())
                    .build()

                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select End Date")
                    .setCalendarConstraints(constraints)
                    .build()

                datePicker.addOnPositiveButtonClickListener { selectedDate ->
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calendar.timeInMillis = selectedDate
                    val dateStr = apiDateFormat.format(calendar.time)

                    // Validate: End date should not be before start date
                    val currentStartDate = startDate
                    if (currentStartDate != null) {
                        try {
                            val selectedDateParsed = apiDateFormat.parse(dateStr)
                            val startDateParsed = apiDateFormat.parse(currentStartDate)
                            if (selectedDateParsed != null && startDateParsed != null && selectedDateParsed.before(
                                    startDateParsed
                                )
                            ) {
                                // Show error dialog
                                DialogHandler.showDialog(
                                    message = "End date cannot be selected before start date",
                                    buttonText = dismiss
                                ) {}
                                return@addOnPositiveButtonClickListener
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "OrdersScreen",
                                "Error validating dates: ${e.message}"
                            )
                        }
                    }

                    viewModel.setEndDate(dateStr)
                }

                datePicker.addOnDismissListener {
                    showEndDatePicker = false
                }

                datePicker.show(activity.supportFragmentManager, "EndDatePicker")
            } catch (e: Exception) {
                android.util.Log.e("OrdersScreen", "Error showing end date picker: ${e.message}")
                showEndDatePicker = false
            }
        } else if (showEndDatePicker && activity == null) {
            android.util.Log.e(
                "OrdersScreen",
                "Activity is not FragmentActivity, cannot show date picker"
            )
            showEndDatePicker = false
        }
    }

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
        if (currentRoute == "orders") {
            viewModel.uiEvent.collect { event ->
                // Double-check we're still on orders screen before processing events
                if (navController.currentBackStackEntry?.destination?.route == "orders") {
                    when (event) {
                        is OrdersUiEvent.ShowLoading -> Loader.show("Loading...")
                        is OrdersUiEvent.HideLoading -> Loader.hide()
                        is OrdersUiEvent.ShowNoInternetDialog -> {
                            DialogHandler.showDialog(
                                message = event.message,
                                buttonText = dismiss,
                                iconRes = R.drawable.no_internet_icon
                            ) {}
                        }

                        is OrdersUiEvent.ShowError -> {
                            DialogHandler.showDialog(
                                message = event.message,
                                buttonText = "OK"
                            ) {}
                        }

                        is OrdersUiEvent.ShowSuccess -> {
                            DialogHandler.showDialog(
                                message = event.message,
                                buttonText = "OK",
                                iconRes = R.drawable.success_circle_icon,
                            ) {}
                        }

                        is OrdersUiEvent.NavigateToRefundInvoice -> {
                            // Navigate to RefundInvoiceScreen with invoice number
                            // Note: We'll need to pass refund data via ViewModel state or navigation argument
                            // For now, using invoice number as identifier
                            navController.navigate("refund_invoice/${event.refundData.invoiceNo}")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header App Bar
        HeaderAppBar(
            title = "Orders",
            onLogout = {
                showLogoutDialog = true
            },
            userName = userName,
            isClockedIn = isClockedIn,
            clockInTime = clockInTime
        )

        // Main Content: Left Panel (Order List) + Right Panel (Order Details)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel - Order List
            Column(
                modifier = Modifier
                    .weight(if (selectedOrder != null) 1f else 1f)
                    .fillMaxHeight()
            ) {
                // Date Range Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Date
                    Column(modifier = Modifier.weight(0.8f)) {
                        BaseText(
                            text = "Start Date",
                            fontSize = 12f,
                            color = Color.Gray,
                            fontFamily = GeneralSans,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) {
                                    showStartDatePicker = true
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BaseText(
                                text = startDate?.let {
                                    try {
                                        val date = apiDateFormat.parse(it)
                                        date?.let { displayDateFormat.format(it) } ?: it
                                    } catch (e: Exception) {
                                        it
                                    }
                                } ?: "Select Start Date",
                                fontSize = 14f,
                                color = if (startDate != null) Color.Black else Color.Gray,
                                fontFamily = GeneralSans
                            )
                        }
                    }

                    // End Date
                    Column(modifier = Modifier.weight(0.8f)) {
                        BaseText(
                            text = "End Date",
                            fontSize = 12f,
                            color = Color.Gray,
                            fontFamily = GeneralSans,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) {
                                    showEndDatePicker = true
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BaseText(
                                text = endDate?.let {
                                    try {
                                        val date = apiDateFormat.parse(it)
                                        date?.let { displayDateFormat.format(it) } ?: it
                                    } catch (e: Exception) {
                                        it
                                    }
                                } ?: "Select End Date",
                                fontSize = 14f,
                                color = if (endDate != null) Color.Black else Color.Gray,
                                fontFamily = GeneralSans
                            )
                        }
                    }

                    // Apply Button
                    BaseButton(
                        text = "Apply",
                        modifier = Modifier
                            .width(120.dp)
                            .padding(top = 20.dp),
                        backgroundColor = colorResource(id = R.color.primary),
                        textColor = Color.White,
                        fontSize = 14,
                        fontWeight = FontWeight.SemiBold,
                        height = 40.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        onClick = {
                            viewModel.loadOrders(searchQuery, reset = true)
                        }
                    )
                }

                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.search_icon),
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    fontFamily = GeneralSans,
                                    color = Color.Black
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (searchQuery.isEmpty()) {
                                            BaseText(
                                                text = "Search by order number",
                                                fontSize = 14f,
                                                color = Color.Gray,
                                                fontFamily = GeneralSans
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Orders List
                if (isLoading && orders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = "Loading...",
                            fontSize = 16f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                    }
                } else if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = "No orders found",
                            fontSize = 16f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                    }
                } else {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorResource(id = R.color.primary))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BaseText(
                            text = "No",
                            fontSize = 12f,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = GeneralSans,
                            modifier = Modifier.width(40.dp)
                        )
                        BaseText(
                            text = "Order Number",
                            fontSize = 12f,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = GeneralSans,
                            modifier = Modifier.weight(1f)
                        )
                        BaseText(
                            text = "Total",
                            fontSize = 12f,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = GeneralSans,
                            modifier = Modifier.width(100.dp)
                        )
                        BaseText(
                            text = "Status",
                            fontSize = 12f,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = GeneralSans,
                            modifier = Modifier.width(100.dp)
                        )
                    }

                    // Orders List
                    LazyColumn {
                        items(filteredOrders) { order ->
                            val isSelected = selectedOrder?.orderNumber == order.orderNumber
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) {
                                            Color(0xFFB3E5FC) // Light blue for selected row
                                        } else {
                                        if (filteredOrders.indexOf(order) % 2 == 0) Color.White else Color(
                                                0xFFE3F2FD
                                        )
                                        }
                                    )
                                    .padding(16.dp)
                                    .clickable {
                                        // Toggle selection: if already selected, deselect; otherwise select
                                        selectedOrder = if (isSelected) null else order
                                    },
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BaseText(
                                    text = "${filteredOrders.indexOf(order) + 1}-",
                                    fontSize = 12f,
                                    color = Color.Black,
                                    fontFamily = GeneralSans,
                                    modifier = Modifier.width(40.dp)
                                )
                                BaseText(
                                    text = order.orderNumber,
                                    fontSize = 12f,
                                    color = Color.Black,
                                    fontFamily = GeneralSans,
                                    modifier = Modifier.weight(1f)
                                )
                                BaseText(
                                    text = "$${
                                        String.format(
                                            "%.2f",
                                            order.total.toDoubleOrNull() ?: 0.0
                                        )
                                    }",
                                    fontSize = 12f,
                                    color = Color.Black,
                                    fontFamily = GeneralSans,
                                    modifier = Modifier.width(100.dp)
                                )
                                // Status Column
                                BaseText(
                                    text = order.status.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                    },
                                    fontSize = 12f,
                                    color = when (order.status.lowercase()) {
                                        "completed" -> colorResource(id = R.color.green_success)
                                        "pending" -> colorResource(id = R.color.orange)
                                        else -> Color.Black
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = GeneralSans,
                                    modifier = Modifier.width(100.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right Panel - Order Details (shown when order is selected)
            selectedOrder?.let { order ->
                OrderDetailsPanel(
                    order = order,
                    onDismiss = { selectedOrder = null },
                    onPrintReceipt = { viewModel.printOrder(order) },
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )
            }
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                onConfirm = {
                    showLogoutDialog = false
                    // Handle logout - navigate to login
                    navController.navigate("pinCode") {
                        popUpTo(0) { inclusive = false }
                    }
                },
                onDismiss = { showLogoutDialog = false }
            )
        }
    }
}

@Composable
fun OrderDetailsPanel(
    order: OrderDetailList,
    onDismiss: () -> Unit,
    onPrintReceipt: () -> Unit,
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    // Refund mode state - reset when order changes (using orderNumber as key)
    var isRefundMode by remember(order.orderNumber) { mutableStateOf(false) }
    var selectedItemIndices by remember(order.orderNumber) { mutableStateOf<Set<Int>>(emptySet()) }
    
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val parsedDate = remember(order.createdAt) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(order.createdAt)
            date?.let { dateFormat.format(it) } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    // Function to toggle item selection
    val onItemClick = { index: Int ->
        selectedItemIndices = if (selectedItemIndices.contains(index)) {
            selectedItemIndices - index
        } else {
            selectedItemIndices + index
        }
    }
    
    // Function to enter refund mode
    val enterRefundMode = {
        isRefundMode = true
        selectedItemIndices = emptySet()
    }
    
    // Function to cancel refund mode
    val cancelRefundMode = {
        isRefundMode = false
        selectedItemIndices = emptySet()
    }
    
    // Calculate totals for selected items (or all items if full refund)
    val calculatedTotals = remember(selectedItemIndices, isRefundMode, order) {
        if (!isRefundMode) {
            // Normal mode: show full order totals
            Triple(
                order.subTotal.toDoubleOrNull() ?: 0.0,
                order.taxValue,
                order.total.toDoubleOrNull() ?: 0.0
            )
        } else {
            val itemsToCalculate = if (selectedItemIndices.isEmpty()) {
                // Full refund: use all items
                order.orderItems
            } else {
                // Partial refund: use selected items
                selectedItemIndices.map { order.orderItems[it] }
            }
            
            // Calculate subtotal
            var subtotal = 0.0
            itemsToCalculate.forEach { item ->
                val itemPrice = item.price.toDoubleOrNull() ?: 0.0
                subtotal += itemPrice * item.quantity
            }
            
            // Calculate discount proportion
            val orderDiscountAmount = order.discountAmount.toDoubleOrNull() ?: 0.0
            val orderSubtotal = order.subTotal.toDoubleOrNull() ?: 0.0
            val discount = if (orderDiscountAmount > 0 && orderSubtotal > 0) {
                (orderDiscountAmount / orderSubtotal) * subtotal
            } else {
                0.0
            }
            
            // Calculate tax proportion
            val orderTax = order.taxValue
            val tax = if (orderSubtotal > 0) {
                (orderTax / orderSubtotal) * subtotal
            } else {
                0.0
            }
            
            // Calculate total
            val total = subtotal - discount + tax
            
            Triple(subtotal, tax, total)
        }
    }
    
    val (displaySubtotal, displayTax, displayTotal) = calculatedTotals
    
    // Calculate discount for display
    val displayDiscount = remember(selectedItemIndices, isRefundMode, order) {
        if (!isRefundMode) {
            order.discountAmount.toDoubleOrNull() ?: 0.0
        } else {
            val orderDiscountAmount = order.discountAmount.toDoubleOrNull() ?: 0.0
            val orderSubtotal = order.subTotal.toDoubleOrNull() ?: 0.0
            val itemsToCalculate = if (selectedItemIndices.isEmpty()) {
                order.orderItems
            } else {
                selectedItemIndices.map { order.orderItems[it] }
            }
            var subtotal = 0.0
            itemsToCalculate.forEach { item ->
                val itemPrice = item.price.toDoubleOrNull() ?: 0.0
                subtotal += itemPrice * item.quantity
            }
            if (orderDiscountAmount > 0 && orderSubtotal > 0) {
                (orderDiscountAmount / orderSubtotal) * subtotal
            } else {
                0.0
            }
        }
    }

    // Function to confirm refund
    val confirmRefund = {
        viewModel.processRefund(order, selectedItemIndices)
    }

    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .fillMaxSize()
    ) {
        // 1. Header (NOT scrollable) - Blue background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.primary))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Order ID Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BaseText(
                        text = "Order ID",
                        fontSize = 13f,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = "#${order.orderNumber}",
                        fontSize = 11f,
                        color = Color.White,
                        fontFamily = GeneralSans
                    )
                }

                // Payment Type Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BaseText(
                        text = "Payment Type",
                        fontSize = 13f,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Bold,
                    )
                    BaseText(
                        text = order.paymentMethod.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        fontSize = 11f,
                        color = Color.White,
                        fontFamily = GeneralSans
                    )
                }

                // Date Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BaseText(
                        text = "Date",
                        fontSize = 13f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = parsedDate,
                        fontSize = 11f,
                        color = Color.White,
                        fontFamily = GeneralSans
                    )
                }
            }
        }

        // 2. Items List (SCROLLABLE AREA) - Only scrollable section
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(order.orderItems.size) { index ->
                OrderDetailItemRow(
                    itemNumber = index + 1,
                    item = order.orderItems[index],
                    isSelected = selectedItemIndices.contains(index),
                    isSelectionMode = isRefundMode,
                    onClick = { onItemClick(index) }
                )
            }
        }

        // 3. Summary Section (NOT scrollable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BaseText(
                    text = "Subtotal",
                    fontSize = 14f,
                    color = colorResource(R.color.grey_text_colour),
                    fontFamily = GeneralSans
                )
                BaseText(
                    text = "$${String.format("%.2f", displaySubtotal)}",
                    fontSize = 14f,
                    color = colorResource(R.color.grey_text_colour),
                    fontFamily = GeneralSans
                )
            }

            // Show discount if it exists
            if (displayDiscount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Discount:",
                        fontSize = 14f,
                        color = colorResource(R.color.grey_text_colour),
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = "-$${String.format("%.2f", displayDiscount)}",
                        fontSize = 14f,
                        color = colorResource(R.color.grey_text_colour),
                        fontFamily = GeneralSans
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BaseText(
                    text = "Tax:",
                    fontSize = 14f,
                    color = colorResource(R.color.grey_text_colour),
                    fontFamily = GeneralSans
                )
                BaseText(
                    text = "$${String.format("%.2f", displayTax)}",
                    fontSize = 14f,
                    color = colorResource(R.color.grey_text_colour),
                    fontFamily = GeneralSans
                )
            }

            // Dashed divider
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Gray,
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BaseText(
                    text = "Total:",
                    fontSize = 16f,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontFamily = GeneralSans
                )
                BaseText(
                    text = "$${String.format("%.2f", displayTotal)}",
                    fontSize = 14f,
                    color = Color.Black,
                    fontFamily = GeneralSans
                )
            }
        }

        // 4. Action Buttons (NOT scrollable) - Fixed at bottom
        if (isRefundMode) {
            // Refund mode buttons: Refund and Cancel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BaseButton(
                    contentPadding = PaddingValues(0.dp),
                    text = "Refund",
                    modifier = Modifier.weight(1f),
                    backgroundColor = colorResource(id = R.color.red_error),
                    textColor = Color.White,
                    fontSize = 12,
                    fontWeight = FontWeight.SemiBold,
                    height = 60.dp,
                    onClick = confirmRefund,
                    textMaxLines = 2
                )
                BaseButton(
                    contentPadding = PaddingValues(0.dp),
                    text = "Cancel",
                    modifier = Modifier.weight(1f),
                    backgroundColor = colorResource(id = R.color.color_dark_blue),
                    textColor = Color.White,
                    fontSize = 12,
                    fontWeight = FontWeight.SemiBold,
                    height = 60.dp,
                    onClick = cancelRefundMode,
                    textMaxLines = 2
                )
            }
        } else {
            // Normal mode buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BaseButton(
                contentPadding = PaddingValues(0.dp),
                text = "Print\nReceipt",
                modifier = Modifier.weight(1f),
                backgroundColor = colorResource(id = R.color.color_dark_blue),
                textColor = Color.White,
                fontSize = 12,
                fontWeight = FontWeight.SemiBold,
                height = 60.dp,
                onClick = onPrintReceipt,
                textMaxLines = 2
            )
            BaseButton(
                contentPadding = PaddingValues(0.dp),
                text = "Email\nReceipt",
                modifier = Modifier.weight(1f),
                backgroundColor = colorResource(id = R.color.color_dark_blue),
                textColor = Color.White,
                fontSize = 12,
                fontWeight = FontWeight.SemiBold,
                height = 60.dp,
                textMaxLines = 2,
                onClick = {
                    DialogHandler.showDialog(
                        message = "Email receipt feature will be implemented soon",
                        buttonText = "OK"
                    ) {}
                }
            )
            BaseButton(
                contentPadding = PaddingValues(0.dp),
                text = "Text\nReceipt",
                modifier = Modifier.weight(1f),
                backgroundColor = colorResource(id = R.color.color_dark_blue),
                textColor = Color.White,
                fontSize = 12,
                fontWeight = FontWeight.SemiBold,
                height = 60.dp,
                textMaxLines = 2,
                onClick = {
                    DialogHandler.showDialog(
                        message = "Text receipt feature will be implemented soon",
                        buttonText = "OK"
                    ) {}
                }
            )
            BaseButton(
                contentPadding = PaddingValues(0.dp),
                text = "Refund",
                modifier = Modifier.weight(1f),
                    backgroundColor = colorResource(id = R.color.color_dark_blue),
                textColor = Color.White,
                fontSize = 12,
                fontWeight = FontWeight.SemiBold,
                height = 60.dp,
                textMaxLines = 2,
                    onClick = enterRefundMode
                )
            }
        }
    }
}

@Composable
fun OrderDetailItemRow(
    itemNumber: Int,
    item: com.retail.dolphinpos.domain.model.home.order_details.OrderItem,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {}
) {
    val firstImageUrl = item.product.images.firstOrNull()?.fileURL
    val variantTitle = when (val variant = item.productVariant) {
        is Map<*, *> -> variant["title"]?.toString() ?: ""
        else -> ""
    }

    // Text colors based on selection state
    val textColor = if (isSelected) Color.White else Color.Black
    val qtyTextColor = if (isSelected) Color.White.copy(alpha = 0.9f) else colorResource(R.color.grey_text_colour)
    val backgroundColor = if (isSelected) colorResource(id = R.color.primary) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
            .then(
                if (isSelectionMode) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item Number or Checkmark (e.g., "1-", "2-", "3-" or checkmark when selected)
        if (isSelectionMode && isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tick),
                contentDescription = "Selected",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        } else {
        BaseText(
            text = "$itemNumber-",
            fontSize = 14f,
            fontWeight = FontWeight.Medium,
                color = textColor,
            fontFamily = GeneralSans,
            //modifier = Modifier.width(32.dp)
        )
        }

        // Product Image (fixed size)
        if (!firstImageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = firstImageUrl,
                contentDescription = item.product.name,
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.LightGray, RoundedCornerShape(4.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.LightGray, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                BaseText(
                    text = "IMG",
                    fontSize = 10f,
                    color = Color.Gray,
                    fontFamily = GeneralSans
                )
            }
        }

        // Product Name and Subtext
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BaseText(
                text = item.product.name,
                fontSize = 14f,
                color = textColor,
                fontFamily = GeneralSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            BaseText(
                text = "Qty: ${item.quantity}",
                fontSize = 12f,
                color = qtyTextColor,
                fontFamily = GeneralSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

        }

        // Price aligned to the right
        BaseText(
            text = "$${String.format("%.2f", item.price.toDoubleOrNull() ?: 0.0)}",
            fontSize = 14f,
            color = textColor,
            fontFamily = GeneralSans,
            textAlign = TextAlign.End,
            modifier = Modifier.width(80.dp)
        )
    }
}

//@Composable
//fun OrderDetailsDialog(
//    order: OrderDetailList,
//    onDismiss: () -> Unit
//) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = {
//            BaseText(
//                text = "Order Details",
//                fontSize = 18f,
//                fontWeight = FontWeight.Bold,
//                color = Color.Black,
//                fontFamily = GeneralSans
//            )
//        },
//        text = {
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .verticalScroll(rememberScrollState()),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                // Order Number
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    BaseText(
//                        text = "Order No:",
//                        fontSize = 14f,
//                        color = Color.Gray,
//                        fontFamily = GeneralSans
//                    )
//                    BaseText(
//                        text = order.orderNumber,
//                        fontSize = 14f,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.Black,
//                        fontFamily = GeneralSans
//                    )
//                }
//
//                // Subtotal
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    BaseText(
//                        text = "Subtotal:",
//                        fontSize = 14f,
//                        color = Color.Gray,
//                        fontFamily = GeneralSans
//                    )
//                    BaseText(
//                        text = "$${String.format("%.2f", order.subTotal.toDoubleOrNull() ?: 0.0)}",
//                        fontSize = 14f,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.Black,
//                        fontFamily = GeneralSans
//                    )
//                }
//
//                // Discount
//                if (order.discountAmount.toDoubleOrNull() ?: 0.0 > 0) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        BaseText(
//                            text = "Discount:",
//                            fontSize = 14f,
//                            color = Color.Gray,
//                            fontFamily = GeneralSans
//                        )
//                        BaseText(
//                            text = "-$${
//                                String.format(
//                                    "%.2f",
//                                    order.discountAmount.toDoubleOrNull() ?: 0.0
//                                )
//                            }",
//                            fontSize = 14f,
//                            fontWeight = FontWeight.SemiBold,
//                            color = Color(0xFF4CAF50),
//                            fontFamily = GeneralSans
//                        )
//                    }
//                }
//
//                // Cash Discount
//                val cashDiscountValue = when (val amount = order.cashDiscountAmount) {
//                    is String -> amount.toDoubleOrNull() ?: 0.0
//                    is Double -> amount
//                    is Int -> amount.toDouble()
//                    is Float -> amount.toDouble()
//                    is Long -> amount.toDouble()
//                    else -> 0.0
//                }
//                if (cashDiscountValue > 0) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        BaseText(
//                            text = "Cash Discount:",
//                            fontSize = 14f,
//                            color = Color.Gray,
//                            fontFamily = GeneralSans
//                        )
//                        BaseText(
//                            text = "-$${String.format("%.2f", cashDiscountValue)}",
//                            fontSize = 14f,
//                            fontWeight = FontWeight.SemiBold,
//                            color = Color(0xFF4CAF50),
//                            fontFamily = GeneralSans
//                        )
//                    }
//                }
//
//                // Tax Breakdown or Tax Exempt
//                if (!order.applyTax) {
//                    // Tax Exempt case
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        BaseText(
//                            text = "Tax:",
//                            fontSize = 14f,
//                            color = Color.Gray,
//                            fontFamily = GeneralSans
//                        )
//                        BaseText(
//                            text = "Exempt",
//                            fontSize = 14f,
//                            fontWeight = FontWeight.SemiBold,
//                            color = Color(0xFF4CAF50),
//                            fontFamily = GeneralSans
//                        )
//                    }
//                } else {
//                    // Show tax breakdown if available from transactions, or show tax value
//                    val hasTaxBreakdown =
//                        order.transactions.isNotEmpty() && order.transactions.any { it.tax > 0 }
//                    val totalTaxFromTransactions = order.transactions.sumOf { it.tax }
//                    val displayTaxValue =
//                        if (totalTaxFromTransactions > 0) totalTaxFromTransactions else order.taxValue
//
//                    if (order.taxValue > 0 || totalTaxFromTransactions > 0) {
//                        if (hasTaxBreakdown && order.transactions.size > 1) {
//                            // Multiple transactions - show breakdown
//                            Column(
//                                modifier = Modifier.fillMaxWidth(),
//                                verticalArrangement = Arrangement.spacedBy(4.dp)
//                            ) {
//                                BaseText(
//                                    text = "Tax Breakdown:",
//                                    fontSize = 14f,
//                                    fontWeight = FontWeight.SemiBold,
//                                    color = Color.Black,
//                                    fontFamily = GeneralSans
//                                )
//                                order.transactions.forEach { transaction ->
//                                    if (transaction.tax > 0) {
//                                        Row(
//                                            modifier = Modifier.fillMaxWidth(),
//                                            horizontalArrangement = Arrangement.SpaceBetween
//                                        ) {
//                                            BaseText(
//                                                text = "${transaction.paymentMethod.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} Tax:",
//                                                fontSize = 13f,
//                                                color = Color.Gray,
//                                                fontFamily = GeneralSans,
//                                                modifier = Modifier.padding(start = 16.dp)
//                                            )
//                                            BaseText(
//                                                text = "$${String.format("%.2f", transaction.tax)}",
//                                                fontSize = 13f,
//                                                fontWeight = FontWeight.Medium,
//                                                color = Color.Black,
//                                                fontFamily = GeneralSans
//                                            )
//                                        }
//                                    }
//                                }
//                                // Total tax
//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.SpaceBetween
//                                ) {
//                                    BaseText(
//                                        text = "Total Tax:",
//                                        fontSize = 14f,
//                                        fontWeight = FontWeight.SemiBold,
//                                        color = Color.Black,
//                                        fontFamily = GeneralSans
//                                    )
//                                    BaseText(
//                                        text = "$${String.format("%.2f", displayTaxValue)}",
//                                        fontSize = 14f,
//                                        fontWeight = FontWeight.SemiBold,
//                                        color = Color.Black,
//                                        fontFamily = GeneralSans
//                                    )
//                                }
//                            }
//                        } else {
//                            // Single transaction or no breakdown - show total tax
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.SpaceBetween
//                            ) {
//                                BaseText(
//                                    text = "Tax:",
//                                    fontSize = 14f,
//                                    color = Color.Gray,
//                                    fontFamily = GeneralSans
//                                )
//                                BaseText(
//                                    text = "$${String.format("%.2f", displayTaxValue)}",
//                                    fontSize = 14f,
//                                    fontWeight = FontWeight.SemiBold,
//                                    color = Color.Black,
//                                    fontFamily = GeneralSans
//                                )
//                            }
//                        }
//                    } else {
//                        // Tax is 0 but applyTax is true (shouldn't happen normally)
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            BaseText(
//                                text = "Tax:",
//                                fontSize = 14f,
//                                color = Color.Gray,
//                                fontFamily = GeneralSans
//                            )
//                            BaseText(
//                                text = "$0.00",
//                                fontSize = 14f,
//                                fontWeight = FontWeight.SemiBold,
//                                color = Color.Black,
//                                fontFamily = GeneralSans
//                            )
//                        }
//                    }
//                }
//
//                // Payment Method
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    BaseText(
//                        text = "Payment Method:",
//                        fontSize = 14f,
//                        color = Color.Gray,
//                        fontFamily = GeneralSans
//                    )
//                    BaseText(
//                        text = order.paymentMethod.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
//                        fontSize = 14f,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.Black,
//                        fontFamily = GeneralSans
//                    )
//                }
//
//                // Status
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    BaseText(
//                        text = "Status:",
//                        fontSize = 14f,
//                        color = Color.Gray,
//                        fontFamily = GeneralSans
//                    )
//                    BaseText(
//                        text = order.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
//                        fontSize = 14f,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.Black,
//                        fontFamily = GeneralSans
//                    )
//                }
//
//                // Created At
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    BaseText(
//                        text = "Created:",
//                        fontSize = 14f,
//                        color = Color.Gray,
//                        fontFamily = GeneralSans
//                    )
//                    BaseText(
//                        text = try {
//                            val inputFormat =
//                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
//                            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
//                            val date = inputFormat.parse(order.createdAt)
//                            outputFormat.format(date ?: java.util.Date())
//                        } catch (e: Exception) {
//                            order.createdAt
//                        },
//                        fontSize = 14f,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.Black,
//                        fontFamily = GeneralSans
//                    )
//                }
//
//                // Divider
//                Spacer(modifier = Modifier.height(2.dp))
//                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
//                Spacer(modifier = Modifier.height(2.dp))
//
//                // Total
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    BaseText(
//                        text = "Total:",
//                        fontSize = 16f,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black,
//                        fontFamily = GeneralSans
//                    )
//                    BaseText(
//                        text = "$${String.format("%.2f", order.total.toDoubleOrNull() ?: 0.0)}",
//                        fontSize = 16f,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black,
//                        fontFamily = GeneralSans
//                    )
//                }
//
//                // Order Items
//                Spacer(modifier = Modifier.height(8.dp))
//                BaseText(
//                    text = "Items:",
//                    fontSize = 14f,
//                    fontWeight = FontWeight.SemiBold,
//                    color = Color.Black,
//                    fontFamily = GeneralSans
//                )
//
//                if (order.orderItems.isNotEmpty()) {
//                    // Header Row
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(colorResource(id = R.color.primary))
//                            .padding(horizontal = 12.dp, vertical = 8.dp),
//                        horizontalArrangement = Arrangement.spacedBy(8.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        BaseText(
//                            text = "Product Name",
//                            fontSize = 12f,
//                            fontWeight = FontWeight.Bold,
//                            color = Color.White,
//                            fontFamily = GeneralSans,
//                            modifier = Modifier.weight(2f)
//                        )
//                        BaseText(
//                            text = "Qty",
//                            fontSize = 12f,
//                            fontWeight = FontWeight.Bold,
//                            color = Color.White,
//                            fontFamily = GeneralSans,
//                            modifier = Modifier.width(60.dp),
//                            textAlign = TextAlign.Center
//                        )
//                        BaseText(
//                            text = "Price",
//                            fontSize = 12f,
//                            fontWeight = FontWeight.Bold,
//                            color = Color.White,
//                            fontFamily = GeneralSans,
//                            modifier = Modifier.width(100.dp),
//                            textAlign = TextAlign.End
//                        )
//                    }
//
//                    LazyColumn(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height((order.orderItems.size * 70).coerceAtMost(300).dp)
//                            .padding(vertical = 8.dp),
//                        verticalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        items(order.orderItems) { item ->
//                            OrderItemRow(item = item)
//                        }
//                    }
//                } else {
//                    BaseText(
//                        text = "No items available",
//                        fontSize = 12f,
//                        color = Color.Gray,
//                        fontFamily = GeneralSans
//                    )
//                }
//            }
//        },
//        confirmButton = {
//            Button(
//                onClick = onDismiss,
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color(0xFF1976D2)
//                )
//            ) {
//                BaseText(
//                    text = "Close",
//                    fontSize = 14f,
//                    fontWeight = FontWeight.SemiBold,
//                    color = Color.White,
//                    fontFamily = GeneralSans
//                )
//            }
//        }
//    )
//}

@Composable
fun OrderItemRow(item: com.retail.dolphinpos.domain.model.home.order_details.OrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Name
        BaseText(
            text = item.product.name,
            fontSize = 13f,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(2f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Quantity
        BaseText(
            text = "${item.quantity}",
            fontSize = 13f,
            color = Color.Black,
            fontFamily = GeneralSans,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )

        // Price Column
        Column(
            modifier = Modifier.width(100.dp),
            horizontalAlignment = Alignment.End
        ) {
            val itemPrice = item.price.toDoubleOrNull() ?: 0.0
            val itemTotal = itemPrice * item.quantity

            // Main Price
            BaseText(
                text = "$${String.format("%.2f", itemTotal)}",
                fontSize = 13f,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                fontFamily = GeneralSans
            )

            // Discount info if available
            if (item.isDiscounted && item.discountedPrice.toDoubleOrNull() ?: 0.0 > 0) {
                val discountedTotal = item.discountedPrice.toDoubleOrNull() ?: 0.0 * item.quantity
                BaseText(
                    text = "Disc: $${String.format("%.2f", discountedTotal)}",
                    fontSize = 11f,
                    color = Color(0xFF4CAF50),
                    fontFamily = GeneralSans
                )
            }
        }
    }
}
