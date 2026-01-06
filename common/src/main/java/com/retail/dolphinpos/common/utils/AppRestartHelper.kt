package com.retail.dolphinpos.common.utils

import android.app.Activity
import android.content.Intent

object AppRestartHelper {
    fun restartApp(activity: Activity) {
        val intent = Intent(activity, activity.javaClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish()
        Runtime.getRuntime().exit(0)
    }
}

