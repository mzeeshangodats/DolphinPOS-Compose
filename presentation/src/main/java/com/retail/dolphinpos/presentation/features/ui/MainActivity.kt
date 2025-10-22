package com.retail.dolphinpos.presentation.features.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.theme.DolphinPosTheme
import com.retail.dolphinpos.presentation.util.ErrorDialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import com.retail.dolphinpos.presentation.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DolphinPosTheme {  // ← Wraps entire app
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(preferenceManager)
                        GlobalLoaderHandler()
                        ErrorDialogHandler.GlobalErrorDialogHost()
                    }
                }
            }
        }
    }

    @Composable
    fun GlobalLoaderHandler() {
        if (Loader.isVisible) {
            Utils.LoaderDialog(message = Loader.message)
        }
    }
}