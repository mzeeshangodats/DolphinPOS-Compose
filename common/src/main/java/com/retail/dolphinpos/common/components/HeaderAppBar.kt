package com.retail.dolphinpos.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retail.dolphinpos.common.R
import com.retail.dolphinpos.common.utils.GeneralSans
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HeaderAppBar(
    title: String = "",
    onLogout: () -> Unit = {},
    userName: String = "",
    isClockedIn: Boolean = false,
    clockInTime: Long = 0L
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(colorResource(id = R.color.primary))
    ) {
        // Logo on the left
        Image(
            painter = painterResource(id = R.drawable.logo_with_bg),
            contentDescription = "Logo",
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterStart)
        )

        // Title in the center
        if (title.isNotEmpty()) {
            androidx.compose.material3.Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GeneralSans,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // User info and logout on the right
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            // User info box (username and clock-in time)
            if (userName.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Top
                ) {
                    // Username
                    androidx.compose.material3.Text(
                        text = userName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 12.sp
                    )
                    
                    // Clock-in status
                    androidx.compose.material3.Text(
                        text = if (isClockedIn && clockInTime > 0L) {
                            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            "Clocked in: ${timeFormat.format(Date(clockInTime))}"
                        } else {
                            "No CheckIn"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 10.sp
                    )
                }
            }
            
            // Logout icon in circular background
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    )
                    .clickable { onLogout() }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.logout_icon),
                        contentDescription = "Logout",
                        modifier = Modifier.size(15.dp),
                        tint = colorResource(id = R.color.primary)
                    )
                }
            }
            
            // Add spacing after the logout button
            Spacer(modifier = Modifier.width(0.dp))
        }
    }
}

