package com.retail.dolphinpos.presentation.features.ui.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun RefundInvoiceScreen(
    navController: NavController,
    invoiceNo: String,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val refundData by viewModel.refundData.collectAsState()

    LaunchedEffect(invoiceNo) {
        // Refund data should already be set when navigating here
        // If not found, navigate back
        if (refundData == null || refundData?.invoiceNo != invoiceNo) {
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.retail.dolphinpos.common.components.HeaderAppBarWithBack(
            title = "Refund Invoice",
            onBackClick = { navController.popBackStack() }
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

            // Content overlay
            refundData?.let { data ->
                RefundInvoiceContent(data = data)
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f),
                        //.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    BaseText(
                        text = "Loading refund details...",
                        fontSize = 16f,
                        fontFamily = GeneralSans,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun RefundInvoiceContent(data: RefundData) {
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
            // Refund Invoice Header
            RefundInvoiceHeader(data = data)

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Refunded Items Section - Scrollable
            Box(
                modifier = Modifier.fillMaxHeight(0.58f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    RefundedItemsSection(items = data.selectedItems)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Refund Summary
            RefundSummarySection(
                subtotal = data.subtotal,
                discount = data.discount,
                tax = data.tax,
                total = data.total
            )
        }
    }
}

@Composable
fun RefundInvoiceHeader(data: RefundData) {
    BaseText(
        text = "Refund Invoice",
        fontSize = 18f,
        fontFamily = GeneralSans,
        fontWeight = FontWeight.Bold,
        color = colorResource(id = R.color.primary)
    )

    Spacer(modifier = Modifier.height(12.dp))

    val dateTime = parseDateTimeRefund(data.order.createdAt)
    val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1.69f)) {
            InfoRow("Refund Invoice No:", "#${data.invoiceNo}")
            InfoRow("Original Order No:", "#${data.order.orderNumber}")
        }
        Column(
            modifier = Modifier.weight(1.5f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoRow("Date:", currentDate)
            InfoRow("Payment Method:", data.order.paymentMethod.replaceFirstChar { it.uppercase() })
        }
        Column(modifier = Modifier.weight(1f)) {
            InfoRow("Time:", currentTime)
        }
    }
}

@Composable
fun RefundedItemsSection(items: List<com.retail.dolphinpos.domain.model.home.order_details.OrderItem>) {
    BaseText(
        text = "Refunded Items",
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
    items.forEach { item ->
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
                text = formatCurrencyRefund(item.price.toDoubleOrNull() ?: 0.0),
                fontSize = 12f,
                fontFamily = GeneralSans,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                color = colorResource(R.color.gray_neutral)
            )
            BaseText(
                text = formatCurrencyRefund((item.price.toDoubleOrNull() ?: 0.0) * item.quantity),
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
fun RefundSummarySection(
    subtotal: Double,
    discount: Double,
    tax: Double,
    total: Double
) {
    Column() {
        InfoRowHorizontalRefund("Subtotal:", formatCurrencyRefund(subtotal))
        if (discount > 0) {
            InfoRowHorizontalRefund("Discount:", formatCurrencyRefund(-discount))
        }
        InfoRowHorizontalRefund("Tax:", formatCurrencyRefund(tax))
        InfoRowHorizontalRefund(
            "Total Refund:",
            formatCurrencyRefund(total),
            isTotal = true
        )
    }

}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BaseText(
            text = label,
            fontSize = 12f,
            fontFamily = GeneralSans,
            color = colorResource(R.color.gray_neutral)
        )
        BaseText(
            text = value,
            fontSize = 12f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}

@Composable
fun InfoRowHorizontalRefund(
    startText: String,
    endText: String,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BaseText(
            text = startText,
            fontSize = if (isTotal) 14f else 12f,
            fontFamily = GeneralSans,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) Color.Black else colorResource(R.color.gray_neutral)
        )
        BaseText(
            text = endText,
            fontSize = if (isTotal) 14f else 12f,
            fontFamily = GeneralSans,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
    }
}

fun formatCurrencyRefund(amount: Double): String {
    return "$${String.format("%.2f", amount)}"
}

fun parseDateTimeRefund(dateTimeString: String): Pair<String, String> {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateTimeString)
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        if (date != null) {
            Pair(dateFormat.format(date), timeFormat.format(date))
        } else {
            Pair("", "")
        }
    } catch (e: Exception) {
        Pair("", "")
    }
}

