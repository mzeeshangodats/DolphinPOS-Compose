package com.retail.dolphinpos.presentation.features.ui.reports.batch_summary

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.components.LogoutConfirmationDialog
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BatchSummaryScreen(
    navController: NavController,
    batchNo: String,
    viewModel: BatchSummaryViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    BatchSummaryContent(
        navController = navController,
        batchNo = batchNo,
        viewModel = viewModel,
        preferenceManager = preferenceManager
    )
}

@Composable
fun BatchSummaryContent(
    navController: NavController,
    batchNo: String,
    viewModel: BatchSummaryViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val batchReport by viewModel.batchReport.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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

    // Load batch report when screen is opened
    LaunchedEffect(batchNo) {
        viewModel.loadBatchReport(batchNo)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BatchSummaryUiEvent.ShowLoading -> {
                    if (!Loader.isVisible) {
                        Loader.show("Please wait while loading batch report")
                    }
                }

                is BatchSummaryUiEvent.HideLoading -> {
                    if (!isLoading) {
                        Loader.hide()
                    }
                }

                is BatchSummaryUiEvent.ShowError -> {
                    Loader.hide()
                    DialogHandler.showDialog(
                        message = event.message, buttonText = "OK"
                    ) {}
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HeaderAppBarWithBack(
            title = "Batch Summary",
            onBackClick = {
                navController.popBackStack()
            }
        )

        // Show content when not loading
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

                                    // Batch Number with blue text
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
                                        InfoRow("Start Amount:", formatCurrency(report.startingCashAmount))
                                        InfoRow("End Amount:", formatCurrency(report.closingCashAmount.toDouble()))
                                        InfoRow("Cash", formatCurrencyString(report.totalCashAmount))
                                        InfoRow("Card", formatCurrencyString(report.totalCardAmount))
                                        InfoRow("Online sale", formatCurrencyString(report.totalOnlineSales))
                                        InfoRow("Discount", formatCurrencyString(report.totalDiscount))
                                        InfoRow("Tax", formatCurrencyString(report.totalTax))
                                        InfoRow("Pay In", formatCurrencyAny(report.totalPayIn))
                                        InfoRow("Pay Out", formatCurrencyAny(report.totalPayOut))
                                        InfoRow("Refund", "$0.00")
                                        InfoRow("Service Charges", formatCurrency(report.totalTip.toDouble()))
                                        InfoRow("Gift Card", "$0.00")
                                        InfoRow("Gift Card Redemption", "$0.00")
                                        InfoRow("Total Sale", formatCurrencyAny(report.totalSales))
                                        InfoRow("Abandoned Carts", report.totalAbandonOrders.toString())
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

