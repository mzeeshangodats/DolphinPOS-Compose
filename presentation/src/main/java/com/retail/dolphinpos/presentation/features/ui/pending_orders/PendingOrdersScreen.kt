package com.retail.dolphinpos.presentation.features.ui.pending_orders

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
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
    
    // Filter orders based on search query
    val filteredOrders = if (searchQuery.isEmpty()) {
        orders
    } else {
        orders.filter { it.orderNo.contains(searchQuery, ignoreCase = true) }
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
                                Box {
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
                            }
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
                                if (filteredOrders.indexOf(order) % 2 == 0) Color.White else Color(0xFFF5F5F5)
                            )
                            .padding(16.dp),
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
                            text = order.orderNo,
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
                            modifier = Modifier.width(80.dp).height(32.dp),
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
    }
}
