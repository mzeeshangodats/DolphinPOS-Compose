package com.retail.dolphinpos.presentation.features.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BottomNavigationBar
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.features.ui.reports.batch_report.BatchReportContent
import com.retail.dolphinpos.presentation.features.ui.reports.batch_history.BatchHistoryContent
import com.retail.dolphinpos.presentation.features.ui.reports.transaction_activity.TransactionActivityContent

@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val menus by viewModel.menus.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedMenuIndex.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
    ) {
        // Content area
        Box(
            modifier = Modifier.weight(1f)
        ) {
            when (selectedIndex) {
                1 -> {
                    // Show Batch Report Screen inline
                    BatchReportContent(navController = navController, preferenceManager = preferenceManager)
                }
                2 -> {
                    // Show Batch History Screen inline
                    BatchHistoryContent(navController = navController, preferenceManager = preferenceManager)
                }
                3 -> {
                    // Show Transaction Activity Screen inline
                    TransactionActivityContent(navController = navController, preferenceManager = preferenceManager)
                }
                0 -> {
                    // If Home is selected, show Batch Report as default
                    BatchReportContent(navController = navController, preferenceManager = preferenceManager)
                }
                else -> {
                    // Default to Batch Report if index is invalid
                    BatchReportContent(navController = navController, preferenceManager = preferenceManager)
                }
            }
        }

        // Bottom Navigation Bar (nested within ReportsScreen)
        BottomNavigationBar(
            menus = menus,
            selectedIndex = selectedIndex,
            onMenuClick = { menu ->
                // Find index by matching destinationId instead of object reference
                val index = menus.indexOfFirst { it.destinationId == menu.destinationId }
                android.util.Log.d("ReportsScreen", "Menu clicked: ${menu.menuName}, destinationId: ${menu.destinationId}, found index: $index")

                if (index >= 0) {
                    // If Home button is clicked (index 0), navigate to home
                    if (index == 0) {
                        navController.navigate("home") {
                            // Pop up to the start destination to clear the back stack properly
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        // Handle sub-navigation for Reports
                        android.util.Log.d("ReportsScreen", "Calling selectMenu with index: $index")
                        viewModel.selectMenu(index)
                    }
                } else {
                    android.util.Log.e("ReportsScreen", "Menu not found in list! Menu: ${menu.menuName}, destinationId: ${menu.destinationId}")
                }
            }
        )
    }
}
