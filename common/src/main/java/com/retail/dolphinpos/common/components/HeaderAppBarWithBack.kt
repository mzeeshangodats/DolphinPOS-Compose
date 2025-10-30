package com.retail.dolphinpos.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retail.dolphinpos.common.R
import com.retail.dolphinpos.common.utils.GeneralSans

@Composable
fun HeaderAppBarWithBack(
    title: String,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(colorResource(id = R.color.primary))
    ) {
        // Back button on the left
        Icon(
            painter = painterResource(id = R.drawable.back_icon),
            contentDescription = "Back",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(24.dp)
                .clickable { onBackClick() },
            tint = Color.White
        )

        // Title in the center
        BaseText(
            text = title,
            fontSize = 16f,
            fontWeight = FontWeight.Bold,
            fontFamily = GeneralSans,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

