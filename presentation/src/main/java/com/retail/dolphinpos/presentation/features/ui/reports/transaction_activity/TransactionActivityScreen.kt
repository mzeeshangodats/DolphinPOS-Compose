package com.retail.dolphinpos.presentation.features.ui.reports.transaction_activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.Loader
import java.text.SimpleDateFormat
import java.util.Locale

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
    var searchQuery by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()

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
                val totalItems = filteredTransactions.size
                if (lastVisibleIndex != null && lastVisibleIndex >= totalItems - 3 && hasMorePages && !isLoadingMore) {
                    viewModel.loadMoreTransactions()
                }
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
            val isTaxExempt = (transaction.tax == null || transaction.tax == 0.0) && 
                              (transaction.taxDetails == null || transaction.taxDetails.isEmpty())
            
            if (isTaxExempt) {
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
                    transaction.taxDetails.forEach { taxDetail ->
                        val taxDescription = when (taxDetail.type?.lowercase()) {
                            "percentage" -> "${taxDetail.title} (${taxDetail.value}%)"
                            "fixed amount" -> "${taxDetail.title} ($${taxDetail.value})"
                            else -> "${taxDetail.title} (${taxDetail.value}%)"
                        }
                        // Use taxDetail.amount if available, otherwise show 0.00
                        val taxAmount = taxDetail.amount ?: 0.0
                        InfoRow(
                            label = taxDescription,
                            value = "$${String.format("%.2f", taxAmount)}"
                        )
                    }
                    // Show total tax if multiple taxes
                    if (transaction.taxDetails.size > 1) {
                        val totalTax = transaction.taxDetails.sumOf { it.amount ?: 0.0 }
                        InfoRow(
                            label = "Total Tax:",
                            value = "$${String.format("%.2f", totalTax)}"
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

