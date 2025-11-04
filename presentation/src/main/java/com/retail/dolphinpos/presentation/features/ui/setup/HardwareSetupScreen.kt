package com.retail.dolphinpos.presentation.features.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.BottomNavigationBar
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R

@Composable
fun HardwareSetupScreen(
    navController: NavController,
    viewModel: HardwareSetupViewModel = hiltViewModel()
) {
    val menus by viewModel.menus.collectAsState()
    val selectedIndex by viewModel.selectedMenuIndex.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
    ) {
        // Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BaseText(
                    text = "Hardware Setup Screen",
                    color = Color.Black,
                    fontSize = 24f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                BaseText(
                    text = "This is the Hardware Setup screen where you can configure hardware devices.",
                    color = Color.Gray,
                    fontSize = 16f,
                    fontFamily = GeneralSans
                )
            }
        }

        // Bottom Navigation Bar (nested within HardwareSetupScreen)
        BottomNavigationBar(
            menus = menus,
            selectedIndex = selectedIndex,
            onMenuClick = { menu ->
                val index = menus.indexOf(menu)
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
                        // Handle sub-navigation for Setup (Printer/Cash Drawer/Scanner setup)
                        viewModel.selectMenu(index)
                    }
                }
            }
        )
    }
}
