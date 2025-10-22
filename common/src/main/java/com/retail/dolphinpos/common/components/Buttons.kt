package com.retail.dolphinpos.common.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retail.dolphinpos.common.R
import com.retail.dolphinpos.common.utils.GeneralSans

@Composable
fun BaseButton(
    text: String,
    modifier: Modifier = Modifier
        .fillMaxWidth(), // 👈 Default behavior
    enabled: Boolean = true,
    backgroundColor: Color? = null, // 👈 Dynamic color parameter
    fontSize: Int = 16, // 👈 Font size parameter in sp
    debounceTimeMs: Long = 500L,
    onClick: () -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    val containerColor = backgroundColor ?: colorResource(id = R.color.primary)

    Button(
        modifier = modifier
            .height(45.dp), // Slightly taller for better UX
        onClick = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= debounceTimeMs) {
                lastClickTime = currentTime
                onClick()
            }
        },
        enabled = enabled,
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 60.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize.sp,
                color = Color.White
            )
        )
    }
}

