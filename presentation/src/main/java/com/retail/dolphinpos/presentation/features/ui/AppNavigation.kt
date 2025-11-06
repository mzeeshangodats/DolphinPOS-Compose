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
import com.retail.dolphinpos.presentation.features.ui.pending_orders.PendingOrdersScreen
import com.retail.dolphinpos.presentation.features.ui.reports.batch_history.BatchHistoryScreen
import com.retail.dolphinpos.presentation.features.ui.reports.batch_report.BatchReportScreen
import com.retail.dolphinpos.presentation.features.ui.reports.transaction_activity.TransactionActivityScreen
import com.retail.dolphinpos.presentation.features.ui.setup.cc_processing.CreditCardProcessingScreen

@Composable
fun AppNavigation(preferenceManager: PreferenceManager) {
    val navController = rememberNavController()

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
        startDestination = "splash"
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

        // Pending Orders Screen
        composable("pending_orders") {
            PendingOrdersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Credit Card Processing Screen
        composable("credit_card_processing") {
            CreditCardProcessingScreen(navController = navController)
        }

        // Batch Report Screen
        composable("batch_report") {
            BatchReportScreen(navController = navController)
        }

        // Batch History Screen
        composable("batch_history") {
            BatchHistoryScreen(navController = navController)
        }

        // Transaction Activity Screen
        composable("transaction_activity") {
            TransactionActivityScreen(navController = navController)
        }
    }
}