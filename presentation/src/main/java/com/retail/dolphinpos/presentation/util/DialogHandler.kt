package com.retail.dolphinpos.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.Utils.CustomErrorDialog

object DialogHandler {

    var showDialog by mutableStateOf<DialogData?>(null)

    fun showDialog(
        message: String,
        buttonText: String = "OK",
        iconRes: Int = R.drawable.cross_red,
        cancellable: Boolean = false,
        onActionClick: (() -> Unit)? = null,
        secondButtonText: String? = null,
        onSecondButtonClick: (() -> Unit)? = null
    ) {
        showDialog = DialogData(
            message = message,
            buttonText = buttonText,
            iconRes = iconRes,
            cancellable = cancellable,
            onActionClick = onActionClick,
            secondButtonText = secondButtonText,
            onSecondButtonClick = onSecondButtonClick
        )
    }

    data class DialogData(
        val message: String,
        val buttonText: String,
        val iconRes: Int,
        val cancellable: Boolean,
        val onActionClick: (() -> Unit)?,
        val secondButtonText: String? = null,
        val onSecondButtonClick: (() -> Unit)? = null
    )

    fun hideDialog() {
        showDialog = null
    }

    @Composable
    fun GlobalDialogHost() {
        showDialog?.let { data ->
            CustomErrorDialog(
                message = data.message,
                buttonText = data.buttonText,
                iconRes = data.iconRes,
                cancellable = data.cancellable,
                onDismiss = { hideDialog() },
                onActionClick = data.onActionClick,
                secondButtonText = data.secondButtonText,
                onSecondButtonClick = data.onSecondButtonClick
            )
        }
    }
}