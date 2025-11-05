package com.retail.dolphinpos.common.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    textColor: Color? = null, // 👈 Text color parameter
    fontSize: Int = 16, // 👈 Font size parameter in sp
    height: Dp? = null, // 👈 Height parameter
    border: BorderStroke? = null, // 👈 Optional border
    cornerRadius: Dp = 5.dp, // 👈 Corner radius parameter
    contentPadding: PaddingValues? = null, // 👈 Content padding parameter
    debounceTimeMs: Long = 500L,
    onClick: () -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    val containerColor = backgroundColor ?: colorResource(id = R.color.primary)
    val finalTextColor = textColor ?: Color.White

    Button(
        modifier = modifier
            .then(height?.let { Modifier.height(it) } ?: Modifier.height(45.dp)),
        onClick = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= debounceTimeMs) {
                lastClickTime = currentTime
                onClick()
            }
        },
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = finalTextColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = finalTextColor.copy(alpha = 0.6f)
        ),
        contentPadding = contentPadding ?: PaddingValues(horizontal = 60.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize.sp,
                color = finalTextColor,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

