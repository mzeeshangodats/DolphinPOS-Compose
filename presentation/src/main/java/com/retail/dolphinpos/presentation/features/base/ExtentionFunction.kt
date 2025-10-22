package com.retail.dolphinpos.presentation.features.base

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast

/** Shows a short-duration Toast message using the Activity context. */
fun Activity.showToast(message: String) {
    try {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } catch (exception: Exception) {
        Log.d("Exception ", exception.message.toString())
    }
}

/** Shows a long-duration Toast message using the Activity context. */
fun Activity.showToastLong(message: String) {
    try {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    } catch (exception: Exception) {
        Log.d("Exception ", exception.message.toString())
    }
}

/** Starts a new Activity of the specified type, optionally transferring data via the Intent. */
inline fun <reified T : Activity> Activity.gotoNextActivity(
    dataBlock: Intent.() -> Unit = {} // New optional parameter for Intent configuration
) {
    val newIntent = Intent(this, T::class.java).apply(dataBlock)
    startActivity(newIntent)
}

// Optional: A version that also finishes the current activity
/** Starts a new Activity and finishes the current one, optionally transferring data. */
inline fun <reified T : Activity> Activity.gotoNextActivityAndFinish(
    dataBlock: Intent.() -> Unit = {}
) {
    val newIntent = Intent(this, T::class.java).apply(dataBlock)
    startActivity(newIntent)
    finish()
}

/**
 * Hides the Status Bar and Navigation Bar to enter full-screen immersive mode.
 */
fun Window.setImmersiveFullScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+ (including Android 15)
        // Use the WindowInsetsController API
        insetsController?.apply {
            // 1. Hide the desired system bars (Status Bar and Navigation Bar)
            hide(WindowInsets.Type.systemBars())

            // 2. Set the behavior: Important for gestures.
            // BEHAVIOR_DEFAULT: Bars reappear on any system gesture.
            // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE: Bars appear as temporary overlays when user swipes.
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        // Legacy method for older APIs (though not necessary for Android 15+ focus)
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
    }
}




