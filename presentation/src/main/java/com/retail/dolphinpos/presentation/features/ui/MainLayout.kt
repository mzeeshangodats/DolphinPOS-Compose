package com.retail.dolphinpos.presentation.features.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.features.ui.home.HomeScreen
import com.retail.dolphinpos.presentation.features.ui.home.HomeViewModel
import com.retail.dolphinpos.common.components.BottomNavigationBar
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.features.ui.inventory.InventoryScreen
import com.retail.dolphinpos.presentation.features.ui.orders.OrdersScreen
import com.retail.dolphinpos.presentation.features.ui.products.ProductsScreen
import com.retail.dolphinpos.presentation.features.ui.reports.ReportsScreen
import com.retail.dolphinpos.presentation.features.ui.setup.HardwareSetupScreen

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
                "orders" -> OrdersScreen(navController = navController)
                "inventory" -> InventoryScreen(navController = navController, preferenceManager = preferenceManager)
                "reports" -> ReportsScreen(navController = navController)
                "setup" -> HardwareSetupScreen(navController = navController)
            }
        }

        // Persistent Bottom Navigation
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
}
