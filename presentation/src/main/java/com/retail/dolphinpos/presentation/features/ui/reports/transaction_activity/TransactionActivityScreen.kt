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
    var searchQuery by remember { mutableStateOf("") }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        TransactionActivityItem(transaction = transaction)
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
            transaction.tax?.let {
                InfoRow("Tax:", "$${String.format("%.2f", it)}")
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

