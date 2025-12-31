package com.retail.dolphinpos.presentation.features.ui.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailList
import com.retail.dolphinpos.presentation.R
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OrderDetailScreen(
    navController: NavController,
    orderNumber: String,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val orderDetail by viewModel.orderDetail.collectAsState()

    LaunchedEffect(orderNumber) {
        viewModel.loadOrderByOrderNumber(orderNumber)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.light_grey)),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        HeaderAppBarWithBack(
            title = "Order Details",
            onBackClick = { navController.navigateUp() }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
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

            ) {
                orderDetail?.let { order ->
                    OrderDetailContent(order = order)
                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = "Loading order details...",
                            fontSize = 16f,
                            fontFamily = GeneralSans,
                            color = Color.Gray
                        )
                    }
                }
            }
        }



    }
}

@Composable
fun OrderDetailContent(order: OrderDetailList) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 60.dp, vertical = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Order Details Section
            OrderDetailsSection(order = order)

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Payment Summary Section - Scrollable
            Box(
                modifier = Modifier.fillMaxHeight(0.58f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    PaymentSummarySection(order = order)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Breakdown
            val subTotal = order.subTotal.toDoubleOrNull() ?: 0.0
            val tax = order.taxValue
            val discount = order.discountAmount.toDoubleOrNull() ?: 0.0

            // Calculate EBT and Cash amounts from transactions
            val ebtAmount = order.transactions
                .filter { it.paymentMethod.lowercase().contains("ebt", ignoreCase = true) }
                .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

            val cashAmount = order.transactions
                .filter { it.paymentMethod.lowercase().contains("cash", ignoreCase = true) }
                .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

            val total = order.total.toDoubleOrNull() ?: 0.0

            InfoRowHorizontal("SubTotal:", formatCurrency(subTotal))
            InfoRowHorizontal("Discount:", formatCurrency(discount))
            InfoRowHorizontal("Tax", formatCurrency(tax))
            if (ebtAmount > 0) {
                InfoRowHorizontal("Amount Charged to EBT Card:", formatCurrency(ebtAmount))
            }
            if (cashAmount > 0) {
                InfoRowHorizontal("Amount Charged to Cash:", formatCurrency(cashAmount))
            }
            InfoRowHorizontal(
                "Total:",
                formatCurrency(total),
                isTotal = true
            )
        }
    }
}

@Composable
fun OrderDetailsSection(order: OrderDetailList) {
    BaseText(
        text = "Order Details",
        fontSize = 18f,
        fontFamily = GeneralSans,
        fontWeight = FontWeight.Bold,
        color = colorResource(id = R.color.primary)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Parse date and time from createdAt
    val dateTime = parseDateTime(order.createdAt)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1.69f)) {
            InfoRow("Order No:", "#${order.orderNumber}")
            InfoRow("Order Type:", order.source.replaceFirstChar { it.uppercase() })

        }
        Column(
            modifier = Modifier.weight(1.5f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoRow("Date:", dateTime.first)
            InfoRow("Transaction Mode:", order.paymentMethod.replaceFirstChar { it.uppercase() })
        }
        Column(modifier = Modifier.weight(1f)) {
            InfoRow("Time:", dateTime.second)

            val cardNumber = order.transactions.firstOrNull()?.cardDetails?.last4
            InfoRow("Card Number:", cardNumber ?: "N/A")
        }
    }
}

@Composable
fun PaymentSummarySection(order: OrderDetailList) {
    BaseText(
        text = "Payment Summary",
        fontSize = 18f,
        fontFamily = GeneralSans,
        fontWeight = FontWeight.Bold,
        color = colorResource(id = R.color.primary)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Items Table Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BaseText(
            text = "Items",
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2f),
            color = Color.Black
        )
        BaseText(
            text = "QTY",
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = Color.Black
        )
        BaseText(
            text = "Price",
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = Color.Black
        )
        BaseText(
            text = "Total",
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = Color.Black
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Items List
    order.orderItems.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BaseText(
                text = item.product.name,
                fontSize = 12f,
                fontFamily = GeneralSans,
                modifier = Modifier.weight(2f),
                maxLines = 2,
                color = colorResource(R.color.gray_neutral)
            )
            BaseText(
                text = String.format("%02d", item.quantity),
                fontSize = 12f,
                fontFamily = GeneralSans,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = colorResource(R.color.gray_neutral)
            )
            BaseText(
                text = formatCurrency(item.price.toDoubleOrNull() ?: 0.0),
                fontSize = 12f,
                fontFamily = GeneralSans,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                color = colorResource(R.color.gray_neutral)
            )
            BaseText(
                text = formatCurrency((item.price.toDoubleOrNull() ?: 0.0) * item.quantity),
                fontSize = 12f,
                fontFamily = GeneralSans,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                color = colorResource(R.color.gray_neutral)
            )
        }
    }


}

@Composable
fun InfoRowHorizontal(
    startText: String,
    endText: String,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BaseText(
            text = startText,
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) colorResource(id = R.color.primary) else Color.Black
        )

        BaseText(
            text = endText,
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isTotal) colorResource(id = R.color.primary) else Color.Black
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isTotal: Boolean = false,
    alignCenter: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = if (alignCenter)
            Alignment.CenterHorizontally
        else
            Alignment.Start
    ) {
        BaseText(
            text = label,
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) colorResource(id = R.color.primary) else Color.Black
        )
        BaseText(
            text = value,
            fontSize = 12f,
            fontFamily = GeneralSans,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isTotal) colorResource(id = R.color.primary) else Color.Black
        )
    }
}

private fun parseDateTime(dateTimeString: String): Pair<String, String> {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateTimeString)

        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)

        val dateStr = date?.let { dateFormat.format(it) } ?: ""
        val timeStr = date?.let { timeFormat.format(it) } ?: ""

        Pair(dateStr, timeStr)
    } catch (e: Exception) {
        Pair("", "")
    }
}

private fun formatCurrency(amount: Double): String {
    return "$" + String.format("%.2f", amount)
}

private fun calculateTaxPercentage(subTotal: Double, tax: Double): String {
    return if (subTotal > 0) {
        String.format("%.0f", (tax / subTotal) * 100)
    } else {
        "0"
    }
}

