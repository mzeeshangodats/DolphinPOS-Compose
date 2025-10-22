package com.retail.dolphinpos.presentation.features.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R

@Composable
fun ProductsScreen(
    navController: NavController,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BaseText(
            text = "Products Screen",
            color = Color.Black,
            fontSize = 24f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BaseText(
            text = "This is the Products screen where you can manage your product inventory.",
            color = Color.Gray,
            fontSize = 16f,
            fontFamily = GeneralSans
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.primary)
            )
        ) {
            BaseText(
                text = "Back to Home",
                color = Color.White,
                fontSize = 16f,
                fontFamily = GeneralSans
            )
        }
    }
}
