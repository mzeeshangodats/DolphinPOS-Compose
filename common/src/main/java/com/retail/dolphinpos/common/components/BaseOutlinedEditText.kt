package com.retail.dolphinpos.common.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.retail.dolphinpos.common.R
import com.retail.dolphinpos.common.utils.GeneralSans

@Composable
fun BaseOutlinedEditText(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Light,
                    color = Color.Gray
                )
            )
        },
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        textStyle = TextStyle(
            fontFamily = GeneralSans,
            color = Color.Black
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = colorResource(id = R.color.borderOutline),
            unfocusedBorderColor = colorResource(id = R.color.borderOutline),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            cursorColor = colorResource(id = R.color.primary)
        )
    )
}
