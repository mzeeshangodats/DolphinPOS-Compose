package com.retail.dolphinpos.presentation.features.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.BottomNavigationBar
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R

@Composable
fun OrdersScreen(
    navController: NavController,
    viewModel: OrdersViewModel = hiltViewModel()
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
                    text = "Orders Screen",
                    color = Color.Black,
                    fontSize = 24f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                BaseText(
                    text = "This is the Orders screen where you can view order history and manage orders.",
                    color = Color.Gray,
                    fontSize = 16f,
                    fontFamily = GeneralSans
                )
            }
        }

        // Bottom Navigation Bar (nested within OrdersScreen)
        BottomNavigationBar(
            menus = menus,
            selectedIndex = selectedIndex,
            onMenuClick = { menu ->
                val index = menus.indexOf(menu)
                if (index >= 0) {
                    // If Home button is clicked (index 0), navigate to home
                    if (index == 0) {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        // Handle sub-navigation for Orders (Pending/Completed/All Orders)
                        viewModel.selectMenu(index)
                    }
                }
            }
        )
    }
}
