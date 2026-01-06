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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.common.utils.AppRestartHelper
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarAuth
import com.retail.dolphinpos.common.components.BackupRestoreProgressDialog
import com.retail.dolphinpos.presentation.features.ui.backup.BackupViewModel
import com.retail.dolphinpos.presentation.features.ui.backup.BackupUiEvent
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

private fun navigateFromSplash(navController: NavController, preferenceManager: PreferenceManager) {
    preferenceManager.setSplashScreenShown(true)
    val isLoggedIn = preferenceManager.isLogin()
    val hasRegister = preferenceManager.getRegister()
    when {
        !isLoggedIn -> navController.navigate("login") {
            popUpTo("splash") { inclusive = true }
        }
        !hasRegister -> navController.navigate("selectRegister") {
            popUpTo("splash") { inclusive = true }
        }
        else -> navController.navigate("pinCode") {
            popUpTo("splash") { inclusive = true }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SplashScreen(
    navController: NavController,
    preferenceManager: PreferenceManager,
    viewModel: SplashViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRestoreProgress by remember { mutableStateOf(false) }
    val isLoading by backupViewModel.isLoading.collectAsStateWithLifecycle()

    // Handle backup UI events
    LaunchedEffect(Unit) {
        try {
            backupViewModel.uiEvent.collect { event ->
                when (event) {
                    is BackupUiEvent.ShowLoading -> showRestoreProgress = true
                    is BackupUiEvent.HideLoading -> {
                        showRestoreProgress = false
                        // Mark restore as completed and navigate
                        preferenceManager.setDatabaseRestoreCompleted(true)
                        navigateFromSplash(navController, preferenceManager)
                    }
                    is BackupUiEvent.ShowError -> {
                        showRestoreProgress = false
                        // Check if error is "Backup file does not exist" - if so, just navigate without showing dialog
                        val isFileNotFound = event.message.contains("Backup file does not exist", ignoreCase = true)
                        // Mark restore as completed even on error (so it doesn't retry)
                        preferenceManager.setDatabaseRestoreCompleted(true)
                        if (isFileNotFound) {
                            // File doesn't exist - just navigate without showing error
                            navigateFromSplash(navController, preferenceManager)
                        } else {
                            // Other errors - show dialog then navigate
                            DialogHandler.showDialog(
                                message = event.message,
                                buttonText = "OK"
                            ) {
                                navigateFromSplash(navController, preferenceManager)
                            }
                        }
                    }
                    is BackupUiEvent.ShowSuccess -> {
                        // Success dialog will be shown, navigation handled in HideLoading
                    }
                    is BackupUiEvent.RestartApp -> {
                        if (context is Activity) {
                            AppRestartHelper.restartApp(context)
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Ignore cancellation - navigation happened, coroutine was cancelled
        }
    }

    SplashScreenContent(
        currentTime = currentTime,
        currentDate = currentDate,
        onStartClick = {
            // Check if restore has been completed
            val isRestoreCompleted = preferenceManager.isDatabaseRestoreCompleted()
            
            if (!isRestoreCompleted) {
                // Check if backup file exists before attempting restore
                coroutineScope.launch {
                    val backupExists = com.retail.dolphinpos.data.datasource.ExternalStorageHelper.checkIfFileExists()
                    if (backupExists) {
                        // Start restore process
                        backupViewModel.restoreDatabaseFromFile()
                        // Flag will be set in event handler after restore completes
                    } else {
                        // Backup file doesn't exist - mark as completed and navigate without restore
                        preferenceManager.setDatabaseRestoreCompleted(true)
                        navigateFromSplash(navController, preferenceManager)
                    }
                }
            } else {
                // Restore already completed - navigate normally
                navigateFromSplash(navController, preferenceManager)
            }
        }
    )

    // Show progress dialog during restore
    if (showRestoreProgress) {
        BackupRestoreProgressDialog(message = "Restoring database...")
    }
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
                .padding(top = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BaseText(
                text = currentTime,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight. Light
            )

            Spacer(modifier = Modifier.height(8.dp))

            BaseText(
                text = currentDate,
                fontSize = 16f,
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
