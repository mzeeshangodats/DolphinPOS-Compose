package com.retail.dolphinpos.presentation.features.base

import android.view.View

class CooldownClickListener(
    private val cooldownTime: Long = 200L,
    private val onSafeClick: (View) -> Unit
) : View.OnClickListener{

    private var lastClickTime: Long = 0L

    override fun onClick(v: View) {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastClickTime) >= cooldownTime) {
            lastClickTime = currentTime
            onSafeClick(v)
        }
    }
}