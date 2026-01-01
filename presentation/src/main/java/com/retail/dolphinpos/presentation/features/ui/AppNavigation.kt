package com.retail.dolphinpos.presentation.features.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.features.ui.auth.cash_denomination.CashDenominationScreen
import com.retail.dolphinpos.presentation.features.ui.auth.login.LoginScreen
import com.retail.dolphinpos.presentation.features.ui.auth.pin_code.PinCodeScreen
import com.retail.dolphinpos.presentation.features.ui.auth.select_register.SelectRegisterScreen
import com.retail.dolphinpos.presentation.features.ui.auth.splash.SplashScreen
import com.retail.dolphinpos.presentation.features.ui.orders.OrderDetailScreen
import com.retail.dolphinpos.presentation.features.ui.products.CreateProductScreen
import com.retail.dolphinpos.presentation.features.ui.reports.batch_history.BatchHistoryScreen
import com.retail.dolphinpos.presentation.features.ui.reports.batch_report.BatchReportScreen
import com.retail.dolphinpos.presentation.features.ui.reports.batch_summary.BatchSummaryScreen
import com.retail.dolphinpos.presentation.features.ui.reports.transaction_activity.TransactionActivityScreen
import com.retail.dolphinpos.presentation.features.ui.setup.cc_processing.CreditCardProcessingScreen
import com.retail.dolphinpos.presentation.features.ui.setup.cfd.CustomerDisplaySetupScreen

@Composable
fun AppNavigation(preferenceManager: PreferenceManager) {
    val navController = rememberNavController()
    
    // Determine start destination: show splash only on first launch
    // But check login status - if not logged in, always show splash or login
    val startDestination = if (preferenceManager.isSplashScreenShown()) {
        // Check if user is logged in before going to pinCode
        val isLoggedIn = preferenceManager.isLogin()
        val hasRegister = preferenceManager.getRegister()
        when {
            !isLoggedIn -> "login"  // Redirect to login if not logged in
            !hasRegister -> "selectRegister"  // Redirect to select register if no register
            else -> "pinCode"  // Only go to pinCode if logged in and has register
        }
    } else {
        "splash"   // Show splash on first launch
    }

    // Check if we need to navigate to selectRegister (after register verification failure)
    LaunchedEffect(Unit) {
        // Check if flag is set (set by background worker when register is not occupied)
        if (preferenceManager.getForceRegisterSelection()) {
            preferenceManager.clearForceRegisterSelection()
            // Navigate to selectRegister
            navController.navigate("selectRegister") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen
        composable("splash") {
            SplashScreen(navController = navController, preferenceManager = preferenceManager)
        }

        // Login Screen
        composable("login") {
            LoginScreen(navController = navController)
        }

        // Select Register Screen
        composable("selectRegister") {
            SelectRegisterScreen(navController = navController)
        }

        // PIN Code Screen
        composable("pinCode") {
            PinCodeScreen(navController = navController)
        }

        // Cash Denomination Screen
        composable(
            route = "cashDenomination/{userId}/{storeId}/{registerId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.IntType },
                navArgument("storeId") { type = NavType.IntType },
                navArgument("registerId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            CashDenominationScreen(navController = navController)
        }

        // Main Layout with Bottom Navigation (Home, Products, Orders, Reports, Setup)
        composable("home") {
            MainLayout(navController = navController, preferenceManager = preferenceManager)
        }

        // Products Screen (accessed through MainLayout)
        composable("products") {
            MainLayout(navController = navController, preferenceManager = preferenceManager)
        }

        // Create product route (without productId)
        composable("create_product") {
            CreateProductScreen(
                navController = navController,
                preferenceManager = preferenceManager,
                productId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Update product route (with productId)
        composable(
            route = "create_product/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId")
            CreateProductScreen(
                navController = navController,
                preferenceManager = preferenceManager,
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Orders Screen (accessed through MainLayout)
        composable("orders") {
            MainLayout(navController = navController, preferenceManager = preferenceManager)
        }

        // Inventory Screen (accessed through MainLayout)
        composable("inventory") {
            MainLayout(navController = navController, preferenceManager = preferenceManager)
        }

        // Reports Screen (accessed through MainLayout)
        composable("reports") {
            MainLayout(navController = navController, preferenceManager = preferenceManager)
        }

        // Hardware Setup Screen (accessed through MainLayout)
        composable("setup") {
            MainLayout(navController = navController, preferenceManager = preferenceManager)
        }

        // Cash Drawer Screen (accessed through MainLayout)
        composable("cash_drawer") {
            MainLayout(navController = navController, preferenceManager = preferenceManager)
        }

        // Credit Card Processing Screen
        composable("credit_card_processing") {
            CreditCardProcessingScreen(navController = navController)
        }

        // Customer Display Setup Screen
        composable("customer_display_setup") {
            CustomerDisplaySetupScreen(navController = navController)
        }

        // Batch Report Screen
        composable("batch_report") {
            BatchReportScreen(navController = navController, preferenceManager = preferenceManager)
        }

        // Batch History Screen
        composable("batch_history") {
            BatchHistoryScreen(navController = navController, preferenceManager = preferenceManager)
        }

        // Batch Summary Screen
        composable(
            route = "batchSummary/{batchNo}",
            arguments = listOf(
                navArgument("batchNo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedBatchNo = backStackEntry.arguments?.getString("batchNo") ?: ""
            val batchNo = try {
                java.net.URLDecoder.decode(encodedBatchNo, "UTF-8")
            } catch (e: Exception) {
                encodedBatchNo
            }
            BatchSummaryScreen(
                navController = navController,
                batchNo = batchNo,
                preferenceManager = preferenceManager
            )
        }

        // Transaction Activity Screen
        composable("transaction_activity") {
            TransactionActivityScreen(navController = navController, preferenceManager = preferenceManager)
        }

        // Order Detail Screen
        composable(
            route = "order_detail/{orderNumber}",
            arguments = listOf(
                navArgument("orderNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderNumber = backStackEntry.arguments?.getString("orderNumber") ?: ""
            OrderDetailScreen(
                navController = navController,
                orderNumber = orderNumber
            )
        }
    }
}