package com.retail.dolphinpos.presentation.features.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.BottomNavigationBar
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.features.ui.home.HomeScreen
import com.retail.dolphinpos.presentation.features.ui.home.HomeViewModel
import com.retail.dolphinpos.presentation.features.ui.inventory.InventoryScreen
import com.retail.dolphinpos.presentation.features.ui.orders.OrdersScreen
import com.retail.dolphinpos.presentation.features.ui.products.ProductsScreen
import com.retail.dolphinpos.presentation.features.ui.reports.ReportsScreen
import com.retail.dolphinpos.presentation.features.ui.setup.HardwareSetupScreen
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val bottomNavMenus by homeViewModel.menus.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    // Determine current selected index based on current destination
    val selectedIndex = remember(currentDestination) {
        val route = currentDestination?.route
        when (route) {
            "home" -> 0
            "products" -> 1
            "orders" -> 2
            "inventory" -> 3
            "reports" -> 4
            "setup" -> 5
            else -> 0
        }
    }

    // Handle back press
    BackHandler(enabled = !showExitDialog) {
        showExitDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
    ) {
        // Main content area
        Box(
            modifier = Modifier.weight(1f)
        ) {
            // Navigation content based on current destination
            when (currentDestination?.route) {
                "home" -> HomeScreen(navController = navController, preferenceManager = preferenceManager)
                "products" -> ProductsScreen(navController = navController, preferenceManager = preferenceManager)
                "orders" -> OrdersScreen(navController = navController, preferenceManager = preferenceManager)
                "inventory" -> InventoryScreen(navController = navController, preferenceManager = preferenceManager)
                "reports" -> ReportsScreen(navController = navController)
                "setup" -> HardwareSetupScreen(navController = navController)
            }
        }

        // Persistent Bottom Navigation (hide on Reports, Setup screens as they have their own nav)
        val currentRoute = currentDestination?.route
        val shouldShowMainNav = currentRoute !in listOf("reports", "setup")
        
        if (shouldShowMainNav) {
            BottomNavigationBar(
                menus = bottomNavMenus,
                selectedIndex = selectedIndex,
                onMenuClick = { menu ->
                    // Map resource IDs to navigation routes
                    val route = when (menu.destinationId) {
                        R.id.homeScreen -> "home"
                        R.id.productsScreen -> "products"
                        R.id.ordersScreen -> "orders"
                        R.id.inventoryScreen -> "inventory"
                        R.id.reportsScreen -> "reports"
                        R.id.setupScreen -> "setup"
                        else -> null
                    }
                    route?.let {
                        navController.navigate(it) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }

        // Exit Confirmation Dialog
        if (showExitDialog) {
            ExitConfirmationDialog(
                onDismiss = { showExitDialog = false },
                onConfirm = {
                    showExitDialog = false
                    if (context is Activity) {
                        context.finish()
                    }
                }
            )
        }
    }
}

@Composable
fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Close button in top right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close_icon),
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Orange circular icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = colorResource(id = R.color.orange),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.info_icon_white),
                            contentDescription = "Info Icon",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title
                    BaseText(
                        text = "Exit App",
                        fontSize = 18F,
                        color = colorResource(id = R.color.colorHint),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Message
                    BaseText(
                        text = "Are you sure you want to close Dolphin POS?",
                        color = colorResource(id = R.color.cart_screen_btn_clr),
                        fontSize = 14F,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel Button (Left)
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = colorResource(id = R.color.cart_screen_btn_clr)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            BaseText(
                                text = "Cancel",
                                color = colorResource(id = R.color.colorHint),
                                fontWeight = FontWeight.Medium,
                                fontSize = 16F
                            )
                        }

                        // Yes Button (Right)
                        Button(
                            onClick = {
                                onDismiss()
                                onConfirm()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.primary)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            BaseText(
                                text = "Yes",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16F
                            )
                        }
                    }
                }
            }
        }
    }
}
