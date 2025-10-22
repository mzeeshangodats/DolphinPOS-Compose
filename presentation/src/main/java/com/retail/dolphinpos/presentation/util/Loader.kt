package com.retail.dolphinpos.presentation.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Loader {
    var isVisible by mutableStateOf(false)
        private set

    var message by mutableStateOf("Please Wait...")

    fun show(messageText: String = "Please Wait...") {
        message = messageText
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }
}