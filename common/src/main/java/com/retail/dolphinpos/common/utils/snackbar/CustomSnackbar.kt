package com.retail.dolphinpos.common.utils.snackbar

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.retail.dolphinpos.common.R

class CustomSnackbar(private val activity: Activity) {

    enum class SnackbarType(val backgroundColor: Int, val textColor: Int) {
        SUCCESS(Color.parseColor("#4CAF50"), Color.WHITE), // Green
        ERROR(Color.parseColor("#F44336"), Color.WHITE),   // Red
        INFO(Color.parseColor("#2196F3"), Color.WHITE),    // Blue
        WARNING(Color.parseColor("#FFC107"), Color.BLACK)  // Yellow
    }

    private val handler = Handler(Looper.getMainLooper())

    fun showSnackbar(
        message: String,
        type: SnackbarType = SnackbarType.INFO,
        duration: Long = 3000L,
        fontResId: Int? = null
    ) {
        val layoutInflater = LayoutInflater.from(activity)
        val snackbarView = layoutInflater.inflate(R.layout.custom_snackbar, null)

        snackbarView.setBackgroundColor(type.backgroundColor)

        val messageTextView = snackbarView.findViewById<TextView>(R.id.snackbar_message)
        messageTextView.text = message
        messageTextView.setTextColor(type.textColor)

        // Apply custom font if provided
        fontResId?.let {
            val typeface = ResourcesCompat.getFont(activity, it)
            messageTextView.typeface = typeface
        }

        val decorView = activity.window.decorView as FrameLayout
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
        }
        decorView.addView(snackbarView, layoutParams)

        val slideDown = TranslateAnimation(0f, 0f, -snackbarView.height.toFloat(), 0f).apply {
            fillAfter = true
        }
        snackbarView.startAnimation(slideDown)

        handler.postDelayed({
                                val slideUp = TranslateAnimation(
                                    0f,
                                    0f,
                                    0f,
                                    -snackbarView.height.toFloat()
                                ).apply {
                                    fillAfter = true
                                    setAnimationListener(object : Animation.AnimationListener {
                                        override fun onAnimationStart(animation: Animation?) {}
                                        override fun onAnimationEnd(animation: Animation?) {
                                            decorView.removeView(snackbarView)
                                        }

                                        override fun onAnimationRepeat(animation: Animation?) {}
                                    })
                                }
                                snackbarView.startAnimation(slideUp)
                            }, duration)
    }
}
