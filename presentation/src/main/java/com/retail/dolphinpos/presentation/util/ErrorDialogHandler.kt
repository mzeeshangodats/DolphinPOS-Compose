package com.retail.dolphinpos.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.Utils.CustomErrorDialog

object ErrorDialogHandler {

    var showErrorDialog by mutableStateOf<ErrorDialogData?>(null)

    fun showError(
        message: String,
        buttonText: String = "OK",
        iconRes: Int = R.drawable.cross_red,
        cancellable: Boolean = false,
        onActionClick: (() -> Unit)? = null
    ) {
        showErrorDialog = ErrorDialogData(message, buttonText, iconRes, cancellable, onActionClick)
    }

    data class ErrorDialogData(
        val message: String,
        val buttonText: String,
        val iconRes: Int,
        val cancellable: Boolean,
        val onActionClick: (() -> Unit)?
    )

    fun hideError() {
        showErrorDialog = null
    }

    @Composable
    fun GlobalErrorDialogHost() {
        showErrorDialog?.let { data ->
            CustomErrorDialog(
                message = data.message,
                buttonText = data.buttonText,
                iconRes = data.iconRes,
                cancellable = data.cancellable,
                onDismiss = { hideError() },
                onActionClick = data.onActionClick
            )
        }
    }
}