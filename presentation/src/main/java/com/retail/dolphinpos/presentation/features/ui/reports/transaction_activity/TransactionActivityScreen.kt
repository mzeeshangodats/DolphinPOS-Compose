package com.retail.dolphinpos.presentation.features.ui.reports.transaction_activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.Loader
import com.retail.dolphinpos.presentation.util.DialogHandler
import androidx.compose.ui.res.stringResource
import com.retail.dolphinpos.domain.model.TaxDetail
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Helper data class for aggregating taxes
private data class TaxDetailWithAmount(
    val taxDetail: TaxDetail,
    val amount: Double
)

@Composable
fun TransactionActivityScreen(
    navController: NavController,
    viewModel: TransactionActivityViewModel = hiltViewModel()
) {
    TransactionActivityContent(navController = navController, viewModel = viewModel)
}

@Composable
fun TransactionActivityContent(
    navController: NavController,
    viewModel: TransactionActivityViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMorePages by viewModel.hasMorePages.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    
    // Date format for display
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

    // Filter transactions based on search query
    val filteredTransactions = if (searchQuery.isEmpty()) {
        transactions
    } else {
        transactions.filter {
            it.invoiceNo?.contains(searchQuery, ignoreCase = true) == true ||
            it.orderNo?.contains(searchQuery, ignoreCase = true) == true ||
            it.paymentMethod.value.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadTransactions()
    }
    
    // Load more when scrolling near the bottom
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                // Use transactions.size instead of filteredTransactions.size for pagination check
                val totalItems = transactions.size
                if (lastVisibleIndex != null && lastVisibleIndex >= totalItems - 3 && hasMorePages && !isLoadingMore && !isLoading) {
                    viewModel.loadMoreTransactions()
                }
            }
    }

    // Clean up loader when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            Loader.hide()
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is TransactionActivityUiEvent.ShowLoading -> Loader.show("Loading...")
                is TransactionActivityUiEvent.HideLoading -> Loader.hide()
                is TransactionActivityUiEvent.ShowError -> {
                    // Handle error
                }
            }
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
                            android.util.Log.e("TransactionActivity", "Error validating dates: ${e.message}")
                        }
                    }
                    
                    viewModel.setStartDate(dateStr)
                }
                
                datePicker.addOnDismissListener {
                    showStartDatePicker = false
                }
                
                datePicker.show(activity.supportFragmentManager, "StartDatePicker")
            } catch (e: Exception) {
                android.util.Log.e("TransactionActivity", "Error showing start date picker: ${e.message}")
                showStartDatePicker = false
            }
        } else if (showStartDatePicker && activity == null) {
            android.util.Log.e("TransactionActivity", "Activity is not FragmentActivity, cannot show date picker")
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
                            android.util.Log.e("TransactionActivity", "Error validating dates: ${e.message}")
                        }
                    }
                    
                    viewModel.setEndDate(dateStr)
                }
                
                datePicker.addOnDismissListener {
                    showEndDatePicker = false
                }
                
                datePicker.show(activity.supportFragmentManager, "EndDatePicker")
            } catch (e: Exception) {
                android.util.Log.e("TransactionActivity", "Error showing end date picker: ${e.message}")
                showEndDatePicker = false
            }
        } else if (showEndDatePicker && activity == null) {
            android.util.Log.e("TransactionActivity", "Activity is not FragmentActivity, cannot show date picker")
            showEndDatePicker = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HeaderAppBarWithBack(
            title = "Transaction Activity",
            onBackClick = { navController.navigate("home") {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            } }
        )
        
        // Date Range Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    viewModel.loadTransactions(reset = true)
                }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                BaseText(
                    text = "Loading transactions...",
                    color = Color.Gray,
                    fontSize = 16f,
                    fontFamily = GeneralSans
                )
            }
        } else {
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    BaseText(
                        text = if (searchQuery.isEmpty()) "No transactions available" else "No transactions found",
                        color = Color.Gray,
                        fontSize = 16f,
                        fontFamily = GeneralSans
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        TransactionActivityItem(transaction = transaction)
                    }
                    
                    // Show loading indicator at the bottom when loading more
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                BaseText(
                                    text = "Loading more...",
                                    color = Color.Gray,
                                    fontSize = 14f,
                                    fontFamily = GeneralSans
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionActivityItem(transaction: TransactionActivityItemData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BaseText(
                    text = transaction.invoiceNo ?: transaction.orderNo ?: "N/A",
                    fontSize = 16f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                BaseText(
                    text = transaction.status,
                    fontSize = 14f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Medium,
                    color = getStatusColor(transaction.status)
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray,
                thickness = 1.dp
            )

            InfoRow("Amount:", "$${String.format("%.2f", transaction.amount)}")
            InfoRow("Payment Method:", transaction.paymentMethod.value.uppercase())
            
            // Tax breakdown, single tax value, or tax exempt
            // Don't show tax breakdown if taxExempt is true
            if (transaction.taxExempt) {
                // Tax Exempt case
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Tax:",
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        color = Color.Gray
                    )
                    BaseText(
                        text = "Exempt",
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            } else if (transaction.taxDetails != null && transaction.taxDetails.isNotEmpty()) {
                // Show tax breakdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BaseText(
                        text = "Tax Breakdown:",
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    
                    // Aggregate taxes by type to avoid duplicates
                    val uniqueTaxMap = mutableMapOf<String, TaxDetailWithAmount>()
                    transaction.taxDetails.forEach { taxDetail ->
                        val taxKey = "${taxDetail.title}_${taxDetail.value}_${taxDetail.type ?: "percentage"}"
                        val taxAmount = taxDetail.amount ?: 0.0
                        
                        val existing = uniqueTaxMap[taxKey]
                        if (existing != null) {
                            // Sum amounts if duplicate tax type found
                            uniqueTaxMap[taxKey] = existing.copy(amount = existing.amount + taxAmount)
                        } else {
                            uniqueTaxMap[taxKey] = TaxDetailWithAmount(taxDetail, taxAmount)
                        }
                    }
                    
                    // Display unique taxes only
                    uniqueTaxMap.values.forEach { taxWithAmount ->
                        val taxDetail = taxWithAmount.taxDetail
                        val taxDescription = when (taxDetail.type?.lowercase()) {
                            "percentage" -> "${taxDetail.title} (${taxDetail.value}%)"
                            "fixed amount" -> "${taxDetail.title} ($${taxDetail.value})"
                            else -> "${taxDetail.title} (${taxDetail.value}%)"
                        }
                        InfoRow(
                            label = taxDescription,
                            value = "$${String.format("%.2f", taxWithAmount.amount)}"
                        )
                    }
                    
                    // Calculate total tax from unique taxes
                    val totalTaxFromDetails = uniqueTaxMap.values.sumOf { it.amount }
                    // Use transaction.tax if available and matches (for verification), otherwise use calculated total
                    val displayTotalTax = transaction.tax?.takeIf { it > 0.0 && kotlin.math.abs(it - totalTaxFromDetails) < 0.01 }
                        ?: totalTaxFromDetails
                    
                    // Show total tax if multiple taxes, or if calculated total differs from transaction.tax
                    if (uniqueTaxMap.size > 1 || (transaction.tax != null && kotlin.math.abs(transaction.tax!! - totalTaxFromDetails) >= 0.01)) {
                        InfoRow(
                            label = "Total Tax:",
                            value = "$${String.format("%.2f", displayTotalTax)}"
                        )
                    }
                }
            } else {
                // Fallback to single tax value
                transaction.tax?.let {
                    if (it > 0) {
                        InfoRow("Tax:", "$${String.format("%.2f", it)}")
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BaseText(
                                text = "Tax:",
                                fontSize = 14f,
                                fontFamily = GeneralSans,
                                color = Color.Gray
                            )
                            BaseText(
                                text = "Exempt",
                                fontSize = 14f,
                                fontFamily = GeneralSans,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                } ?: run {
                    // No tax data available
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Tax:",
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            color = Color.Gray
                        )
                        BaseText(
                            text = "Exempt",
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            InfoRow("Date:", formatTimestamp(transaction.createdAt))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BaseText(
            text = label,
            fontSize = 14f,
            fontFamily = GeneralSans,
            color = Color.Gray
        )
        BaseText(
            text = value,
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

@Composable
fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "paid", "settled" -> colorResource(id = R.color.primary)
        "pending" -> Color.Blue
        "failed", "void" -> Color.Red
        "refund" -> Color.Blue
        else -> Color.Gray
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

