package com.retail.dolphinpos.presentation.features.ui.customer_display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R

@Composable
fun CustomerDisplayScreen(
    navController: NavController,
    viewModel: CustomerDisplayViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val cartData by viewModel.cartData.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
    ) {
        when (connectionState) {
            is CustomerDisplayViewModel.ConnectionState.Disconnected -> {
                ConnectionErrorView(
                    message = "Connecting to POS...",
                    onRetry = { viewModel.connect() }
                )
            }
            is CustomerDisplayViewModel.ConnectionState.Connecting -> {
                ConnectionErrorView(
                    message = "Connecting to POS...",
                    onRetry = null
                )
            }
            is CustomerDisplayViewModel.ConnectionState.Connected -> {
                when {
                    cartData == null -> {
                        // Waiting for data - show welcome or waiting screen
                        WelcomeScreen()
                    }
                    cartData!!.status == "WELCOME" -> {
                        WelcomeScreen()
                    }
                    cartData!!.status == "CHECKOUT_SCREEN" -> {
                        CartDisplayView(cartData = cartData!!)
                    }
                    else -> {
                        // Default to checkout screen if status is unknown
                        CartDisplayView(cartData = cartData!!)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionErrorView(
    message: String,
    onRetry: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BaseText(
            text = message,
            fontSize = 24f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary)
                )
            ) {
                BaseText(
                    text = "Retry",
                    fontSize = 16f,
                    fontFamily = GeneralSans,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BaseText(
            text = "Welcome",
            fontSize = 48f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.primary),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        BaseText(
            text = "Please wait for your order",
            fontSize = 20f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Normal,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CartDisplayView(cartData: com.retail.dolphinpos.data.customer_display.CartDisplayData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                BaseText(
                    text = "Your Order",
                    fontSize = 28f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cart Items
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartData.cartItems) { item ->
                    CartItemRow(item = item)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryRow(
                    label = "Subtotal",
                    amount = cartData.subtotal
                )
                SummaryRow(
                    label = "Tax",
                    amount = cartData.tax
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.LightGray,
                    thickness = 1.dp
                )
                SummaryRow(
                    label = "Total",
                    amount = cartData.total,
                    isTotal = true
                )
            }
        }
    }
}

@Composable
fun CartItemRow(item: com.retail.dolphinpos.domain.model.home.cart.CartItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            BaseText(
                text = item.name ?: "Unknown Product",
                fontSize = 18f,
                fontFamily = GeneralSans,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            BaseText(
                text = "Qty: ${item.quantity} × ${formatCurrency(item.selectedPrice)}",
                fontSize = 14f,
                fontFamily = GeneralSans,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        BaseText(
            text = formatCurrency(item.selectedPrice * item.quantity),
            fontSize = 20f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.primary)
        )
    }
}

@Composable
fun SummaryRow(
    label: String,
    amount: Double,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BaseText(
            text = label,
            fontSize = if (isTotal) 24f else 18f,
            fontFamily = GeneralSans,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
        BaseText(
            text = formatCurrency(amount),
            fontSize = if (isTotal) 24f else 18f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold,
            color = if (isTotal) colorResource(id = R.color.primary) else Color.Black
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return String.format("$%.2f", amount)
}

