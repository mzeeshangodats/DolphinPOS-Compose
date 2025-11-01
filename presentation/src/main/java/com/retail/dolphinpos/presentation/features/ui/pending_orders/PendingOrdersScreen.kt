package com.retail.dolphinpos.presentation.features.ui.pending_orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.data.entities.order.PendingOrderEntity
import com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader

@Composable
fun PendingOrdersScreen(
    viewModel: PendingOrdersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val orders by viewModel.pendingOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedOrder by remember { mutableStateOf<PendingOrderEntity?>(null) }

    // Filter orders based on search query
    val filteredOrders = if (searchQuery.isEmpty()) {
        orders
    } else {
        orders.filter { it.orderNumber.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPendingOrders()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is PendingOrdersUiEvent.ShowLoading -> Loader.show("Loading...")
                is PendingOrdersUiEvent.HideLoading -> Loader.hide()
                is PendingOrdersUiEvent.ShowError -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = "OK"
                    ) {}
                }

                is PendingOrdersUiEvent.ShowSuccess -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = "OK"
                    ) {}
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header with back button and title
        HeaderAppBarWithBack(
            title = "Pending Orders",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

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
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (searchQuery.isEmpty()) {
                                        BaseText(
                                            text = "Search by order number",
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

            // Orders List
            if (isLoading) {
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
            } else if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    BaseText(
                        text = "No pending orders",
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
                        .background(Color(0xFF1976D2))
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
                        text = "Order Number",
                        fontSize = 12f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        modifier = Modifier.weight(1f)
                    )
                    BaseText(
                        text = "Total",
                        fontSize = 12f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        modifier = Modifier.width(100.dp)
                    )
                    BaseText(
                        text = "Action",
                        fontSize = 12f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        modifier = Modifier.width(80.dp)
                    )
                }

                // Orders List
                LazyColumn {
                    items(filteredOrders) { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (filteredOrders.indexOf(order) % 2 == 0) Color.White else Color(
                                        0xFFF5F5F5
                                    )
                                )
                                .padding(16.dp)
                                .clickable { selectedOrder = order },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BaseText(
                                text = "${filteredOrders.indexOf(order) + 1}-",
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.width(40.dp)
                            )
                            BaseText(
                                text = order.orderNumber,
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.weight(1f)
                            )
                            BaseText(
                                text = "$${String.format("%.2f", order.total)}",
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.width(100.dp)
                            )
                            Button(
                                onClick = { viewModel.syncOrder(order.id) },
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Sync",
                                    fontFamily = GeneralSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pending Order Details Dialog
        selectedOrder?.let { order ->
            PendingOrderDetailsDialog(
                order = order,
                onDismiss = { selectedOrder = null }
            )
        }
    }
}

@Composable
fun PendingOrderDetailsDialog(
    order: PendingOrderEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            BaseText(
                text = "Order Details",
                fontSize = 18f,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = GeneralSans
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Order Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Order No:",
                        fontSize = 14f,
                        color = Color.Gray,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = order.orderNumber,
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                }

                // Subtotal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Subtotal:",
                        fontSize = 14f,
                        color = Color.Gray,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = "$${String.format("%.2f", order.subTotal)}",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                }

                // Cash Discount
                if (order.cashDiscountAmount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Cash Discount:",
                            fontSize = 14f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = "-$${String.format("%.2f", order.cashDiscountAmount)}",
                            fontSize = 14f,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50),
                            fontFamily = GeneralSans
                        )
                    }
                }

                // Discount
                if (order.discountAmount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Discount:",
                            fontSize = 14f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = "-$${String.format("%.2f", order.discountAmount)}",
                            fontSize = 14f,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50),
                            fontFamily = GeneralSans
                        )
                    }
                }

                // Reward Discount
                if (order.rewardDiscount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Reward Discount:",
                            fontSize = 14f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = "-$${String.format("%.2f", order.rewardDiscount)}",
                            fontSize = 14f,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50),
                            fontFamily = GeneralSans
                        )
                    }
                }

                // Tax
                if (order.taxValue > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Tax:",
                            fontSize = 14f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = "$${String.format("%.2f", order.taxValue)}",
                            fontSize = 14f,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontFamily = GeneralSans
                        )
                    }
                }

                // Payment Method
//                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Payment Method:",
                        fontSize = 14f,
                        color = Color.Gray,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = order.paymentMethod.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                }

                // Divider
                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(2.dp))

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Total:",
                        fontSize = 16f,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = "$${String.format("%.2f", order.total)}",
                        fontSize = 16f,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                }

                // Order Items
                Spacer(modifier = Modifier.height(8.dp))
                BaseText(
                    text = "Items:",
                    fontSize = 14f,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    fontFamily = GeneralSans
                )

                val orderItems = remember(order.items) {
                    try {
                        val gson = Gson()
                        val type = object : TypeToken<List<CheckOutOrderItem>>() {}.type
                        gson.fromJson(order.items, type)
                    } catch (e: Exception) {
                        emptyList<CheckOutOrderItem>()
                    }
                }

                if (orderItems.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(orderItems) { item ->
                            OrderItemRow(item = item)
                        }
                    }
                } else {
                    BaseText(
                        text = "No items available",
                        fontSize = 12f,
                        color = Color.Gray,
                        fontFamily = GeneralSans
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                )
            ) {
                BaseText(
                    text = "Close",
                    fontSize = 14f,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = GeneralSans
                )
            }
        }
    )
}

@Composable
fun OrderItemRow(item: CheckOutOrderItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Item Name
        BaseText(
            text = item.name ?: "Unknown Item",
            fontSize = 13f,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            fontFamily = GeneralSans,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Quantity
            BaseText(
                text = "Qty: ${item.quantity ?: 0}",
                fontSize = 12f,
                color = Color.Gray,
                fontFamily = GeneralSans
            )

            // Price - Use discountedPrice if available, otherwise use price
            val itemQuantity = item.quantity ?: 0
            val finalPrice = if (item.discountedPrice != null && item.discountedPrice!! > 0.0) {
                item.discountedPrice!!
            } else {
                item.price ?: 0.0
            }
            val itemTotal = finalPrice * itemQuantity
            BaseText(
                text = "$${String.format("%.2f", itemTotal)}",
                fontSize = 12f,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                fontFamily = GeneralSans
            )
        }

        // Discount amount info if available
        if (item.discountedAmount != null && item.discountedAmount!! > 0.0) {
            val discountText = when (item.discountType) {
                "percentage" -> {
                    val discountPercent = ((item.discountedAmount!! / (item.price ?: 1.0)) * 100.0)
                    "-${String.format("%.0f", discountPercent)}% ($${
                        String.format(
                            "%.2f",
                            item.discountedAmount
                        )
                    })"
                }

                "amount" -> {
                    "-$${String.format("%.2f", item.discountedAmount)}"
                }

                else -> {
                    "-$${String.format("%.2f", item.discountedAmount)}"
                }
            }
            BaseText(
                text = "Discount: $discountText",
                fontSize = 11f,
                color = Color(0xFF4CAF50),
                fontFamily = GeneralSans
            )
        }
    }
}
