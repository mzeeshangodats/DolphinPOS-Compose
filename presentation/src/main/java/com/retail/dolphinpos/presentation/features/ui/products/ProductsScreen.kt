package com.retail.dolphinpos.presentation.features.ui.products

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
import androidx.navigation.NavController
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBar
import com.retail.dolphinpos.common.components.LogoutConfirmationDialog
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import java.util.Locale

@Composable
fun ProductsScreen(
    navController: NavController,
    viewModel: ProductsViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager,
) {
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Products?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Get username and clock-in status from preferences
    val userName = preferenceManager.getName()
    val isClockedIn = preferenceManager.isClockedIn()
    val clockInTime = preferenceManager.getClockInTime()

    // Filter products based on search query
    val filteredProducts = if (searchQuery.isEmpty()) {
        products
    } else {
        products.filter { 
            it.name?.contains(searchQuery, ignoreCase = true) == true ||
            it.barCode?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAllProducts()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProductsUiEvent.ShowLoading -> Loader.show("Loading...")
                is ProductsUiEvent.HideLoading -> Loader.hide()
                is ProductsUiEvent.NavigateToLogin -> {
                    navController.navigate("pinCode") {
                        popUpTo(0) { inclusive = false }
                    }
                }
                is ProductsUiEvent.ShowError -> {
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
        // Header App Bar
        HeaderAppBar(
            title = "Products",
            onLogout = {
                showLogoutDialog = true
            },
            userName = userName,
            isClockedIn = isClockedIn,
            clockInTime = clockInTime
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
                                            text = "Search by product name",
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

            // Products List
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
            } else if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    BaseText(
                        text = "No products found",
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
                        text = "Product Name",
                        fontSize = 12f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        modifier = Modifier.weight(1f)
                    )
                    BaseText(
                        text = "Price",
                        fontSize = 12f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        modifier = Modifier.width(100.dp)
                    )
                    BaseText(
                        text = "Quantity",
                        fontSize = 12f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GeneralSans,
                        modifier = Modifier.width(80.dp)
                    )
                }

                // Products List
                LazyColumn {
                    items(filteredProducts) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (filteredProducts.indexOf(product) % 2 == 0) Color.White else Color(
                                        0xFFF5F5F5
                                    )
                                )
                                .padding(16.dp)
                                .clickable { selectedProduct = product },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BaseText(
                                text = "${filteredProducts.indexOf(product) + 1}-",
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.width(40.dp)
                            )
                            BaseText(
                                text = product.name ?: "Unknown Product",
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.weight(1f)
                            )
                            BaseText(
                                text = "$${String.format(Locale.US, "%.2f", product.cashPrice.toDoubleOrNull() ?: 0.0)}",
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.width(100.dp)
                            )
                            BaseText(
                                text = "${product.quantity}",
                                fontSize = 12f,
                                color = Color.Black,
                                fontFamily = GeneralSans,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }
                }
            }
        }

        // Product Details Dialog
        selectedProduct?.let { product ->
            ProductDetailsDialog(
                product = product,
                onDismiss = { selectedProduct = null }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                onDismiss = { showLogoutDialog = false },
                onConfirm = {
                    showLogoutDialog = false
                    viewModel.logout()
                }
            )
        }
    }

    // Show global dialog
    DialogHandler.GlobalDialogHost()
}

@Composable
fun ProductDetailsDialog(
    product: Products,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            BaseText(
                text = "Product Details",
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
                // Product Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Product Name:",
                        fontSize = 14f,
                        color = Color.Gray,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = product.name ?: "Unknown",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                fontFamily = GeneralSans
            )
        }

                // Description
                if (!product.description.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Description:",
                            fontSize = 14f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = product.description!!,
                            fontSize = 14f,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontFamily = GeneralSans,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Barcode
                if (!product.barCode.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Barcode:",
                            fontSize = 14f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = product.barCode!!,
                            fontSize = 14f,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontFamily = GeneralSans
                        )
                    }
                }

                // Quantity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Quantity:",
                        fontSize = 14f,
                        color = Color.Gray,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = "${product.quantity}",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                }

                // Cash Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Cash Price:",
                        fontSize = 14f,
                        color = Color.Gray,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = "$${String.format(Locale.US, "%.2f", product.cashPrice.toDoubleOrNull() ?: 0.0)}",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                }

                // Card Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Card Price:",
                        fontSize = 14f,
                        color = Color.Gray,
                        fontFamily = GeneralSans
                    )
                    BaseText(
                        text = "$${String.format(Locale.US, "%.2f", product.cardPrice.toDoubleOrNull() ?: 0.0)}",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                }

                // Status
                if (!product.status.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Status:",
                            fontSize = 14f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = product.status!!,
                            fontSize = 14f,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontFamily = GeneralSans
                        )
                    }
                }

                // Variants
                if (product.variants != null && product.variants!!.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(2.dp))
                    BaseText(
                        text = "Variants:",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                    product.variants!!.forEach { variant ->
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
                            BaseText(
                                text = variant.title ?: "Unknown Variant",
                                fontSize = 13f,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black,
                                fontFamily = GeneralSans
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BaseText(
                                    text = "Cash: $${String.format(Locale.US, "%.2f", variant.cashPrice?.toDoubleOrNull() ?: 0.0)}",
                                    fontSize = 12f,
                                    color = Color.Gray,
                                    fontFamily = GeneralSans
                                )
                                BaseText(
                                    text = "Card: $${String.format(Locale.US, "%.2f", variant.cardPrice?.toDoubleOrNull() ?: 0.0)}",
                                    fontSize = 12f,
                                    color = Color.Gray,
                                    fontFamily = GeneralSans
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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
