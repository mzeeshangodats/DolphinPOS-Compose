package com.retail.dolphinpos.presentation.features.ui.reports.batch_history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.fragment.app.FragmentActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBar
import com.retail.dolphinpos.common.components.LogoutConfirmationDialog
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun BatchHistoryScreen(
    navController: NavController,
    viewModel: BatchHistoryViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    BatchHistoryContent(navController = navController, viewModel = viewModel, preferenceManager = preferenceManager)
}

@Composable
fun BatchHistoryContent(
    navController: NavController,
    viewModel: BatchHistoryViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val batches by viewModel.batches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
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

    // Filter batches based on search query
    val filteredBatches = if (searchQuery.isEmpty()) {
        batches
    } else {
        batches.filter {
            it.batchNo.contains(searchQuery, ignoreCase = true) ||
            it.status.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            // Debounce search - reload after user stops typing
            kotlinx.coroutines.delay(500)
            viewModel.loadBatchHistory(searchQuery)
        } else {
            viewModel.loadBatchHistory()
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
                            if (selectedDateParsed != null && endDateParsed != null && selectedDateParsed.after(endDateParsed)) {
                                // Show error dialog
                                DialogHandler.showDialog(
                                    message = "Start date cannot be selected after end date",
                                    buttonText = dismiss
                                ) {}
                                return@addOnPositiveButtonClickListener
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BatchHistoryScreen", "Error validating dates: ${e.message}")
                        }
                    }
                    
                    viewModel.setStartDate(dateStr)
                }

                datePicker.addOnDismissListener {
                    showStartDatePicker = false
                }

                datePicker.show(activity.supportFragmentManager, "StartDatePicker")
            } catch (e: Exception) {
                android.util.Log.e("BatchHistoryScreen", "Error showing start date picker: ${e.message}")
                showStartDatePicker = false
            }
        } else if (showStartDatePicker && activity == null) {
            android.util.Log.e("BatchHistoryScreen", "Activity is not FragmentActivity, cannot show date picker")
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
                            if (selectedDateParsed != null && startDateParsed != null && selectedDateParsed.before(startDateParsed)) {
                                // Show error dialog
                                DialogHandler.showDialog(
                                    message = "End date cannot be selected before start date",
                                    buttonText = dismiss
                                ) {}
                                return@addOnPositiveButtonClickListener
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BatchHistoryScreen", "Error validating dates: ${e.message}")
                        }
                    }
                    
                    viewModel.setEndDate(dateStr)
                }

                datePicker.addOnDismissListener {
                    showEndDatePicker = false
                }

                datePicker.show(activity.supportFragmentManager, "EndDatePicker")
            } catch (e: Exception) {
                android.util.Log.e("BatchHistoryScreen", "Error showing end date picker: ${e.message}")
                showEndDatePicker = false
            }
        } else if (showEndDatePicker && activity == null) {
            android.util.Log.e("BatchHistoryScreen", "Activity is not FragmentActivity, cannot show date picker")
            showEndDatePicker = false
        }
    }

    // Handle UI events
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    DisposableEffect(currentRoute) {
        onDispose {
            // Hide loader when leaving this screen
            Loader.hide()
        }
    }
    
    LaunchedEffect(currentRoute) {
        if (currentRoute == "batchHistory") {
            viewModel.uiEvent.collect { event ->
                // Double-check we're still on batch history screen before processing events
                if (navController.currentBackStackEntry?.destination?.route == "batchHistory") {
                    when (event) {
                        is BatchHistoryUiEvent.ShowLoading -> Loader.show("Loading...")
                        is BatchHistoryUiEvent.HideLoading -> Loader.hide()
                        is BatchHistoryUiEvent.ShowNoInternetDialog -> {
                            DialogHandler.showDialog(
                                message = event.message,
                                buttonText = dismiss,
                                iconRes = R.drawable.no_internet_icon
                            ) {}
                        }
                        is BatchHistoryUiEvent.ShowError -> {
                            DialogHandler.showDialog(
                                message = event.message,
                                buttonText = "OK"
                            ) {}
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
            title = "Batch History",
            onLogout = {
                showLogoutDialog = true
            },
            userName = userName,
            isClockedIn = isClockedIn,
            clockInTime = clockInTime
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                                interactionSource = remember { MutableInteractionSource() }
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
                                interactionSource = remember { MutableInteractionSource() }
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
                        viewModel.loadBatchHistory(searchQuery, reset = true)
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
                                            text = "Search by Batch Number",
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

            // Batches List
            if (isLoading && batches.isEmpty()) {
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
            } else if (filteredBatches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    BaseText(
                        text = "No batches found",
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
                        text = "Batch Number",
                        fontSize = 12f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        modifier = Modifier.weight(1f)
                    )
                    BaseText(
                        text = "Cashier",
                        fontSize = 12f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        modifier = Modifier.width(120.dp)
                    )
                    BaseText(
                        text = "Closing Time",
                        fontSize = 12f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        modifier = Modifier.width(120.dp)
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

                // Batches List
                LazyColumn {
                    items(filteredBatches) { batch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (filteredBatches.indexOf(batch) % 2 == 0) Color.White else Color(
                                        0xFFF5F5F5
                                    )
                                )
                                .clickable {
                                    // Navigate to BatchSummary screen with batch number
                                    // URL encode batchNo to handle special characters
                                    val encodedBatchNo = java.net.URLEncoder.encode(batch.batchNo, "UTF-8")
                                    navController.navigate("batchSummary/$encodedBatchNo")
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BaseText(
                                text = "${filteredBatches.indexOf(batch) + 1}-",
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.width(40.dp)
                            )
                            BaseText(
                                text = batch.batchNo,
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.weight(1f)
                            )
                            // Cashier Column - extract name from closed object
                            BaseText(
                                text = extractCashierName(batch.closed),
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.width(120.dp)
                            )
                            // Closing Time Column
                            BaseText(
                                text = formatClosingTime(batch.closingTime),
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.width(120.dp)
                            )
                            // Status Column
                            BaseText(
                                text = batch.status.replaceFirstChar { 
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                                },
                                fontSize = 12f,
                                color = when (batch.status.lowercase()) {
                                    "closed" -> colorResource(id = R.color.green_success)
                                    "open" -> colorResource(id = R.color.primary)
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

/**
 * Helper function to extract cashier name from closed object
 */
private fun extractCashierName(closed: Any?): String {
    if (closed == null) return "-"
    
    return when (closed) {
        is com.retail.dolphinpos.domain.model.report.batch_report.Closed -> closed.name
        is Map<*, *> -> (closed["name"] as? String) ?: "-"
        is String -> {
            // Try to parse as JSON if it's a string
            if (closed.isEmpty()) {
                "-"
            } else {
                try {
                    val gson = Gson()
                    val closedObj = gson.fromJson(closed, com.retail.dolphinpos.domain.model.report.batch_report.Closed::class.java)
                    closedObj.name
                } catch (e: Exception) {
                    "-"
                }
            }
        }
        else -> "-"
    }
}

/**
 * Helper function to format closing time
 */
private fun formatClosingTime(closingTime: Any?): String {
    if (closingTime == null) return "-"
    
    return when (closingTime) {
        is String -> {
            if (closingTime.isEmpty()) {
                "-"
            } else {
                try {
                    // Try to parse and format the time
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val date = inputFormat.parse(closingTime)
                    date?.let { outputFormat.format(it) } ?: closingTime
                } catch (e: Exception) {
                    // If parsing fails, try other formats
                    try {
                        val inputFormat2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        val date = inputFormat2.parse(closingTime)
                        date?.let { outputFormat.format(it) } ?: closingTime
                    } catch (e2: Exception) {
                        closingTime
                    }
                }
            }
        }
        else -> "-"
    }
}
