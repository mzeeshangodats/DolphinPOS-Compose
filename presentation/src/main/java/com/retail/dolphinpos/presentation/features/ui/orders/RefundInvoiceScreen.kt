package com.retail.dolphinpos.presentation.features.ui.orders

import androidx.annotation.ColorRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.domain.model.home.order_details.OrderItem
import com.retail.dolphinpos.presentation.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun RefundInvoiceScreen(
    navController: NavController,
    refundDataJson: String
) {
    var refundData by remember { mutableStateOf<RefundData?>(null) }

    LaunchedEffect(refundDataJson) {
        // Parse refund data from JSON string
        try {
            val gson = Gson()
            refundData = gson.fromJson(refundDataJson, RefundData::class.java)
        } catch (e: Exception) {
            android.util.Log.e("RefundInvoiceScreen", "Error parsing refund data: ${e.message}", e)
            // If parsing fails, navigate back
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderAppBarWithBack(
            title = "Refund Invoice",
            onBackClick = { navController.popBackStack() }
        )
        // "Invoice" heading at top left
        BaseText(
            text = "Invoice",
            fontSize = 20f,
            fontWeight = FontWeight.Bold,
            fontFamily = GeneralSans,
            color = Color.Black,
            modifier = Modifier
                .padding(start = 20.dp, top = 20.dp)
                .align(Alignment.Start)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(1f)
        ) {

            // Content overlay
            refundData?.let { data ->
                RefundInvoiceContent(data = data)
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f),
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
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 60.dp, vertical = 30.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Section - Receipt Section with batch_report_bg
        ReceiptCardSection(data = data)

        Spacer(modifier = Modifier.width(40.dp))

        // Right Section - Receipt Delivery with white background
        ReceiptDeliverySection()
    }
}

@Composable
fun ReceiptCardSection(data: RefundData) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            // .width(360.dp)
            .fillMaxHeight()
            .fillMaxWidth(0.45f)
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.batch_report_bg),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(50.dp)
        ) {
            // Center content horizontally
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Section
                RefundCardHeader(total = data.total)

                Spacer(modifier = Modifier.height(24.dp))

                // Product Details Section
                ProductDetailsSection(items = data.selectedItems, scrollState)

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Details Section
                PaymentDetailsSection(
                    subtotal = data.subtotal,
                    tax = data.tax,
                    discount = data.discount,
                    total = data.total
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun RefundCardHeader(total: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Purple rounded square icon container
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color(0xFF7C3AED),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Refund icon - using cart icon as placeholder, can be replaced with refund icon
            Icon(
                painter = painterResource(id = R.drawable.ic_refund_icon),
                contentDescription = "Refund",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title: "Refund"
        BaseText(
            text = "Refund",
            fontSize = 16f,
            fontWeight = FontWeight.Medium,
            fontFamily = GeneralSans,
            color = colorResource(R.color.grey_text_colour)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Amount
        BaseText(
            text = formatCurrencyRefund(total),
            fontSize = 20f,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = Color.Black
        )
    }
}

@Composable
fun ProductDetailsSection(items: List<OrderItem>, scrollState: ScrollState) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Dashed divider above
        DashedDivider()

        Spacer(modifier = Modifier.height(12.dp))

        // Title: "Product Details" - Fixed (not scrollable)
        BaseText(
            text = "Product Details",
            fontSize = 14f,
            fontWeight = FontWeight.Medium,
            fontFamily = GeneralSans,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Table Header - Fixed (not scrollable)
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            BaseText(
                text = "Item",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                color = Color.Black,
                modifier = Modifier.weight(0.6f)
            )
            BaseText(
                text = "QTY",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                color = Color.Black,
                modifier = Modifier.weight(0.2f),
                textAlign = TextAlign.Center
            )
            BaseText(
                text = "Price",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneralSans,
                color = Color.Black,
                modifier = Modifier.weight(0.2f),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Items List - Scrollable with fixed height
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Fixed height for scrollable area
                .verticalScroll(scrollState)
        ) {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    BaseText(
                        text = item.product.name,
                        fontSize = 12f,
                        fontFamily = GeneralSans,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.weight(0.6f),
                        maxLines = 2
                    )
                    BaseText(
                        text = String.format("%02d", item.quantity),
                        fontSize = 12f,
                        fontFamily = GeneralSans,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.weight(0.2f),
                        textAlign = TextAlign.Center
                    )
                    BaseText(
                        text = formatCurrencyRefund(
                            (item.price.toDoubleOrNull() ?: 0.0) * item.quantity
                        ),
                        fontSize = 12f,
                        fontFamily = GeneralSans,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.weight(0.2f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dashed divider below
        DashedDivider()
    }
}

@Composable
fun PaymentDetailsSection(
    subtotal: Double,
    tax: Double,
    discount: Double,
    total: Double
) {
    Column {
        BaseText(
            text = "Payment Details",
            fontSize = 14f,
            fontWeight = FontWeight.Medium,
            fontFamily = GeneralSans,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        // SubTotal
        PaymentDetailRow("SubTotal", formatCurrencyRefund(subtotal))

        Spacer(modifier = Modifier.height(8.dp))

        // Tax
        PaymentDetailRow("Tax", formatCurrencyRefund(tax))

        Spacer(modifier = Modifier.height(8.dp))

        // Discount
        PaymentDetailRow("Discount", formatCurrencyRefund(discount))

        Spacer(modifier = Modifier.height(12.dp))

        // Net Amount (Final row)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BaseText(
                text = "Net Amount",
                fontSize = 14f,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneralSans,
                color = colorResource(R.color.primary)
            )
            BaseText(
                text = formatCurrencyRefund(total),
                fontSize = 14f,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneralSans,
                color = colorResource(R.color.primary)
            )
        }
    }
}

@Composable
fun PaymentDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BaseText(
            text = label,
            fontSize = 12f,
            fontFamily = GeneralSans,
            color = colorResource(R.color.grey_text_colour)
        )
        BaseText(
            text = value,
            fontSize = 12f,
            fontFamily = GeneralSans,
            color = colorResource(R.color.grey_text_colour)
        )
    }
}

@Composable
fun DashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
        drawLine(
            color = Color(0xFFD1D5DB),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect
        )
    }
}

@Composable
fun ReceiptDeliverySection() {
    var email by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .fillMaxHeight()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title
        BaseText(
            text = "How would you like your receipt?",
            fontSize = 18f,
            fontWeight = FontWeight.Medium,
            fontFamily = GeneralSans,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email Input Section
        Column {
            BaseText(
                text = "Email",
                fontSize = 14f,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneralSans,
                color = colorResource(R.color.primary),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = {
                        BaseText(
                            text = "Enter mail",
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            color = Color.Gray
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { /* send action */ }
                                .padding(end = 8.dp)
                        ) {
                            BaseText(
                                text = "Sent",
                                fontSize = 14f,
                                fontFamily = GeneralSans,
                                color = Color(0xFF2563EB) // blue
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF2563EB)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFF2563EB)
                    ),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = GeneralSans
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )


                // "Sent →" clickable text
                BaseText(
                    text = "Sent →",
                    fontSize = 14f,
                    fontFamily = GeneralSans,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable {
                            // Handle send email
                            android.util.Log.d("RefundInvoiceScreen", "Send email: $email")
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }
        }

       // Spacer(modifier = Modifier.height(8.dp))

        // Print Button
        OutlinedButton(
            onClick = {
                // Handle print
                android.util.Log.d("RefundInvoiceScreen", "Print receipt")
            },
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF2563EB)
            ),
            border = BorderStroke(
                1.dp,
                Color(0xFF2563EB)
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_store),
                    contentDescription = "Print",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                BaseText(
                    text = "Print",
                    fontSize = 14f,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GeneralSans,
                    color = Color.Gray
                )
            }
        }
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

