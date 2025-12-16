package com.retail.dolphinpos.presentation.features.ui.reports.batch_report

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.window.Dialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBar
import com.retail.dolphinpos.common.components.LogoutConfirmationDialog
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BatchReportScreen(
    navController: NavController, 
    viewModel: BatchReportViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    BatchReportContent(navController = navController, viewModel = viewModel, preferenceManager = preferenceManager)
}

@Composable
fun BatchReportContent(
    navController: NavController, 
    viewModel: BatchReportViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val batchReport by viewModel.batchReport.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showClosingCashDialog by viewModel.showClosingCashDialog.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Get username and clock-in status from preferences
    val userName = preferenceManager.getName()
    val isClockedIn = preferenceManager.isClockedIn()
    val clockInTime = preferenceManager.getClockInTime()

    // Clean up loader when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            Loader.hide()
        }
    }

    // Use isLoading state directly to show/hide loader for batch report loading
    LaunchedEffect(isLoading) {
        if (isLoading) {
            Loader.show("Please wait while loading batch report")
        } else {
            Loader.hide()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadBatchReport()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BatchReportUiEvent.ShowLoading -> {
                    // Only show if not already showing (to avoid duplicate messages)
                    if (!Loader.isVisible) {
                        Loader.show("Please wait while loading batch report")
                    }
                }

                is BatchReportUiEvent.HideLoading -> {
                    // Only hide if isLoading is false (to avoid hiding during other operations)
                    if (!isLoading) {
                        Loader.hide()
                    }
                }

                is BatchReportUiEvent.ShowError -> {
                    Loader.hide()
                    DialogHandler.showDialog(
                        message = event.message, buttonText = "OK"
                    ) {}
                }

                is BatchReportUiEvent.NavigateToPinCode -> {
                    Loader.hide()
                    navController.navigate("pinCode")
                }

                is BatchReportUiEvent.NavigateToCashDenomination -> {
                    Loader.hide()
                    // Get required IDs for navigation
                    val userId = preferenceManager.getUserID()
                    val storeId = preferenceManager.getStoreID()
                    val registerId = preferenceManager.getOccupiedRegisterID()
                    navController.navigate("cashDenomination/$userId/$storeId/$registerId") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HeaderAppBar(
            title = "Batch Report",
            onLogout = {
                showLogoutDialog = true
            },
            userName = userName,
            isClockedIn = isClockedIn,
            clockInTime = clockInTime
        )

        // Show content when not loading (Loader is shown via uiEvent)
        if (!isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(colorResource(id = R.color.light_grey))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // End Of Batch Button - positioned below header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 0.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        BaseButton(
                            text = "End Of Batch",
                            onClick = { viewModel.showClosingCashDialog() },
                            enabled = !isLoading,
                            backgroundColor = colorResource(R.color.primary),
                            textColor = Color.White,
                            modifier = Modifier.wrapContentWidth()
                        )
                    }

                    // Batch Report Card with background image - centered
                    batchReport?.let { report ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Container Box for image and content - same size
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .fillMaxHeight(1f)
                            ) {
                                // Background image
                                Image(
                                    painter = painterResource(id = R.drawable.batch_report_bg),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Content overlay positioned inside the image bounds
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 60.dp, vertical = 30.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(15.dp))

                                    // Batch Number with blue underlined text
                                    Column {
                                        BaseText(
                                            text = "Batch Number: ${report.batchNo}",
                                            fontSize = 15f,
                                            fontFamily = GeneralSans,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorResource(R.color.primary),
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(15.dp))

                                    // Financial Details List - scrollable if needed
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        InfoRow(
                                            "Start Amount:",
                                            formatCurrency(report.startingCashAmount)
                                        )
                                        InfoRow(
                                            "End Amount:",
                                            formatCurrency(report.closingCashAmount.toDouble())
                                        )
                                        InfoRow(
                                            "Cash", formatCurrencyString(report.totalCashAmount)
                                        )
                                        InfoRow(
                                            "Card", formatCurrencyString(report.totalCardAmount)
                                        )
                                        InfoRow(
                                            "Online sale",
                                            formatCurrencyString(report.totalOnlineSales)
                                        )
                                        InfoRow(
                                            "Discount", formatCurrencyString(report.totalDiscount)
                                        )
                                        InfoRow("Tax", formatCurrencyString(report.totalTax))
                                        InfoRow("Pay In", formatCurrencyAny(report.totalPayIn))
                                        InfoRow("Pay Out", formatCurrencyAny(report.totalPayOut))
                                        InfoRow("Refund", "$0.00")
                                        InfoRow(
                                            "Service Charges",
                                            formatCurrency(report.totalTip.toDouble())
                                        )
                                        InfoRow("Gift Card", "$0.00")
                                        InfoRow("Gift Card Redemption", "$0.00")
                                        InfoRow("Total Sale", formatCurrencyAny(report.totalSales))
                                        InfoRow(
                                            "Abandoned Carts", report.totalAbandonOrders.toString()
                                        )
                                        InfoRow("Transactions", report.totalTransactions.toString())
                                    }
                                }
                            }
                        }
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            BaseText(
                                text = "No batch report available",
                                color = Color.Gray,
                                fontSize = 16f,
                                fontFamily = GeneralSans
                            )
                        }
                    }
                }
            }
        }

        // Closing Cash Amount Dialog
        if (showClosingCashDialog) {
            // Calculate default closing cash amount as sum of totalCashAmount and startingCashAmount
            val defaultClosingAmount = batchReport?.let { report ->
                val totalCash = report.totalCashAmount?.toDoubleOrNull() ?: 0.0
                val startingCash = report.startingCashAmount.toDouble()
                totalCash + startingCash
            } ?: 0.0

            ClosingCashAmountDialog(
                onDismiss = { viewModel.dismissClosingCashDialog() },
                onConfirm = { amount, shouldClosePaxBatch ->
                    viewModel.closeBatch(amount, shouldClosePaxBatch)
                },
                defaultAmount = defaultClosingAmount,
                batchStatus = batchReport?.status
            )
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
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BaseText(
            text = label,
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.grey_text_colour)
        )
        Spacer(modifier = Modifier.weight(1f))
        BaseText(
            text = value,
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(R.color.grey_text_colour)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun formatCurrency(amount: Double): String {
    return "$" + String.format("%.2f", amount)
}

private fun formatCurrencyString(amount: String?): String {
    return try {
        val value = amount?.toDoubleOrNull() ?: 0.0
        formatCurrency(value)
    } catch (e: Exception) {
        "$0.00"
    }
}

private fun formatCurrencyAny(amount: Any): String {
    return when (amount) {
        is String -> formatCurrencyString(amount)
        is Number -> formatCurrency(amount.toDouble())
        else -> "$0.00"
    }
}

@Composable
fun ClosingCashAmountDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit,
    defaultAmount: Double = 0.0,
    batchStatus: String? = null
) {
    var closingCashAmount by remember(defaultAmount, batchStatus) {
        mutableStateOf(
            if (defaultAmount > 0) String.format(
                "%.2f", defaultAmount
            ) else ""
        )
    }
    var errorMessage by remember(batchStatus) { mutableStateOf<String?>(null) }
    var shouldClosePaxBatch by remember { mutableStateOf(true) } // Checked by default

    // Reset values when batch is closed
    LaunchedEffect(batchStatus) {
        if (batchStatus?.lowercase() == "closed") {
            closingCashAmount = if (defaultAmount > 0) String.format(
                "%.2f", defaultAmount
            ) else ""
            errorMessage = null
            shouldClosePaxBatch = true // Reset to checked
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BaseText(
                    text = "Enter Closing Cash Amount",
                    fontSize = 18f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                BaseOutlinedEditText(
                    value = closingCashAmount, onValueChange = { newValue ->
                        // Allow only numbers and one decimal point
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            closingCashAmount = newValue
                            errorMessage = null
                        }
                    }, placeholder = "0.00", modifier = Modifier.fillMaxWidth()
                )

                // PAX Batch Close Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = shouldClosePaxBatch,
                        onCheckedChange = { shouldClosePaxBatch = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    BaseText(
                        text = "Close Pax Batch (must be on the same network with BroadPOS running)",
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        color = Color.Black,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage != null) {
                    BaseText(
                        text = errorMessage!!,
                        fontSize = 12f,
                        fontFamily = GeneralSans,
                        color = Color.Red
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BaseButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        backgroundColor = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    BaseButton(
                        text = "Confirm",
                        onClick = {
                            val amount = closingCashAmount.toDoubleOrNull()
                            if (amount == null || amount < 0) {
                                errorMessage = "Please enter a valid amount"
                            } else {
                                onConfirm(amount, shouldClosePaxBatch)
                            }
                        },
                        backgroundColor = colorResource(R.color.primary),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
