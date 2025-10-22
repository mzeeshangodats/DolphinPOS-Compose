package com.retail.dolphinpos.presentation.features.ui.auth.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarAuth
import com.retail.dolphinpos.presentation.R

@Composable
fun SplashScreen(
    navController: NavController,
    preferenceManager: PreferenceManager,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    SplashScreenContent(
        currentTime = currentTime,
        currentDate = currentDate,
        onStartClick = {
            val isLoggedIn = preferenceManager.isLogin()
            val hasRegister = preferenceManager.getRegister()
            navController.navigate("login")
            when {
                !isLoggedIn -> navController.navigate("login")
                !hasRegister -> navController.navigate("selectRegister")
                else -> navController.navigate("pinCode")
            }
        }
    )
}

@Composable
private fun SplashScreenContent(
    currentTime: String,
    currentDate: String,
    onStartClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splash_background_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        HeaderAppBarAuth()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BaseText(
                text = currentTime,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(8.dp))

            BaseText(
                text = currentDate,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(20.dp))
            Spacer(modifier = Modifier.height(20.dp))

            BaseButton(
                text = stringResource(id = R.string.let_s_start),
                modifier = Modifier.width(220.dp),
                onClick = onStartClick
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=480"
)
@Composable
fun SplashScreenPreview() {
    MaterialTheme {
        SplashScreenContent(
            currentTime = "09:43 AM",
            currentDate = "Friday, 10 Oct 2025",
            onStartClick = {}
        )
    }
}
