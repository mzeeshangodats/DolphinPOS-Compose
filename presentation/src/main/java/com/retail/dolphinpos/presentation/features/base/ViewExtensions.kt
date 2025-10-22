package com.retail.dolphinpos.presentation.features.base

import android.view.View

fun View.setOnSafeClickListener(
    cooldownTime: Long = 1000L,
    onSafeClick: (View) -> Unit
) {
    setOnClickListener(CooldownClickListener(cooldownTime, onSafeClick))
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}