package com.retail.dolphinpos.presentation.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.features.ui.MainActivity

object Utils {

    /** ====================== 🌀 Loader Dialog ====================== */
    @Composable
    fun LoaderDialog(message: String = "Please Wait...") {
        Dialog(onDismissRequest = { /* Do nothing — non-cancellable */ }) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotating Logo
                    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing)
                        ),
                        label = "rotation"
                    )

                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(45.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BaseText(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }


    @Composable
    fun CustomErrorDialog(
        message: String,
        buttonText: String = "OK",
        iconRes: Int = R.drawable.cross_red,
        cancellable: Boolean = false,
        onDismiss: () -> Unit,
        onActionClick: (() -> Unit)? = null
    ) {
        if (!cancellable) {
            Dialog(onDismissRequest = { /* Disabled cancel */ }) {
                DialogContent(
                    message = message,
                    buttonText = buttonText,
                    iconRes = iconRes,
                    onDismiss = onDismiss,
                    onActionClick = onActionClick
                )
            }
        } else {
            Dialog(onDismissRequest = onDismiss) {
                DialogContent(
                    message = message,
                    buttonText = buttonText,
                    iconRes = iconRes,
                    onDismiss = onDismiss,
                    onActionClick = onActionClick
                )
            }
        }
    }

    @Composable
    private fun DialogContent(
        message: String,
        buttonText: String,
        iconRes: Int,
        onDismiss: () -> Unit,
        onActionClick: (() -> Unit)? = null
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button (top-right)
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_icon),
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                // Icon
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = "Error Icon",
                    modifier = Modifier
                        .size(60.dp)
                        .padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
                // Message

                BaseText(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    fontSize = 18F,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Visible
                )

                // Button
                Button(
                    onClick = {
                        onDismiss()
                        onActionClick?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.primary)),
                    modifier = Modifier
                        .padding(vertical = 20.dp)
                        .wrapContentWidth()
                ) {
                    BaseText(
                        text = buttonText,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }


    /** ====================== 🚪 Logout Dialog ====================== */
    @Composable
    fun LogoutDialog(
        context: Context = LocalContext.current,
        onConfirm: () -> Unit = { performLogout(context) },
        onDismiss: (() -> Unit)? = null
    ) {
        AlertDialog(
            onDismissRequest = { onDismiss?.invoke() },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Yes", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss?.invoke() }) {
                    Text("Cancel")
                }
            }
        )
    }

    /** ====================== 🚀 Perform Logout ====================== */
    private fun performLogout(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "pin")
        }
        context.startActivity(intent)
        if (context is Activity) context.finish()
    }

    /** ====================== 💡 Button Alpha ====================== */
    @Composable
    fun Modifier.enabledWithAlpha(enabled: Boolean): Modifier {
        return this
            .alpha(if (enabled) 1f else 0.4f)
    }
}
