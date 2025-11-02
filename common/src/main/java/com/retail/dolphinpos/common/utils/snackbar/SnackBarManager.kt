package com.retail.dolphinpos.common.utils.snackbar

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat.getFont
import com.google.android.material.snackbar.Snackbar
import com.retail.dolphinpos.domain.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs


private var initialX = 0f
private var initialY = 0f
private val SWIPE_THRESHOLD = 100

@Singleton
class SnackBarManager @Inject constructor() {

    enum class SnackBarType(val backgroundColor: Int) {
        SUCCESS(Color.parseColor("#4CAF50")),
        ERROR(Color.parseColor("#F44336")),
        INFO(Color.parseColor("#2196F3")),
        WARNING(Color.parseColor("#FFC107"))
    }

    private var currentSnackbar: Snackbar? = null

    fun showErrorSnackBar(rootView: View, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        showSnackBar(rootView, message, SnackBarType.ERROR, duration = duration)
    }

    fun showSuccessSnackBar(
        rootView: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        showSnackBar(rootView, message, SnackBarType.SUCCESS, duration = duration)
    }

    fun showInfoSnackBar(rootView: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        showSnackBar(rootView, message, SnackBarType.INFO, duration = duration)
    }

    fun showWarningSnackBar(
        rootView: View,
        message: String,
        duration: Int = Snackbar.LENGTH_LONG
    ) {
        showSnackBar(rootView, message, SnackBarType.WARNING, duration = duration)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showSnackBar(
        rootView: View,
        message: String,
        type: SnackBarType = SnackBarType.INFO,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null,
        fontResId: Int? = com.retail.dolphinpos.common.R.font.general_sans_regular,
        isMultiline: Boolean = true

    ) {
        currentSnackbar?.dismiss()

        currentSnackbar = Snackbar.make(rootView, message, duration).apply {
            setBackgroundTint(type.backgroundColor)

            actionText?.let {
                setAction(it) { action?.invoke() }
                setActionTextColor(Color.WHITE)
            }

            val snackBarView = this.view
            val snackBarText =
                snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            snackBarText.setTextColor(Color.WHITE)
            snackBarText.textSize = 16f

            fontResId?.let {
                val typeface = Typeface.create(getFont(context, it), Typeface.NORMAL)
                snackBarText.typeface = typeface
            }

            if (isMultiline) {
                snackBarText.maxLines = 5
                snackBarText.isSingleLine = false
            }

            snackBarText.gravity = Gravity.CENTER_VERTICAL
            snackBarText.setPadding(16, 0, 16, 0)
            snackBarText.typeface = snackBarText.typeface


            val params = snackBarView.layoutParams
            if (params is ViewGroup.MarginLayoutParams) {
                params.topMargin = 50
                snackBarView.layoutParams = params
            }

            snackBarView.translationY = -snackBarView.height.toFloat()
            snackBarView.requestLayout()

            snackBarView.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = event.rawX
                        initialY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialX
                        val deltaY = event.rawY - initialY

                        if (abs(deltaX) > SWIPE_THRESHOLD) {
                            dismiss()
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            }

            addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (currentSnackbar == transientBottomBar) {
                        currentSnackbar = null
                    }
                }
            })

            addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (currentSnackbar == transientBottomBar) {
                        currentSnackbar = null
                    }
                }
            })
            show()
        }
    }
}
