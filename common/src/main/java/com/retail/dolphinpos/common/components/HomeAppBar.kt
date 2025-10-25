package com.retail.dolphinpos.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.retail.dolphinpos.common.R
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeAppBar(
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    searchResults: List<Products> = emptyList(),
    onProductClick: (Products) -> Unit = {},
    userName: String = "",
    isClockedIn: Boolean = false,
    clockInTime: Long = 0L
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Main App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(colorResource(id = R.color.primary)),
            contentAlignment = Alignment.CenterStart
        ) {
            // Logo on the left
            Image(
                painter = painterResource(id = R.drawable.logo_with_bg),
                contentDescription = "Logo",
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
            )

            // Search Bar in the center
            Box(
                modifier = Modifier
                    .width(500.dp)
                    .height(35.dp)
                    .align(Alignment.Center)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = GeneralSans,
                        fontSize = 12.sp,
                        color = Color.Black
                    ),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.search_icon),
                                contentDescription = "Search",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search Items",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontFamily = GeneralSans
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
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
                        Text(
                            text = userName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = GeneralSans,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            lineHeight = 12.sp
                        )
                        
                        // Clock-in status
                        Text(
                            text = if (isClockedIn && clockInTime > 0L) {
                                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                "Clocked in: ${timeFormat.format(Date(clockInTime))}"
                            } else {
                                "No CheckIn"
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = GeneralSans,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            lineHeight = 10.sp
                        )
                    }
                }
                
                // Logout icon in circular background with padding
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onLogout() }
                        .padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.logout_icon),
                            contentDescription = "Logout",
                            modifier = Modifier.size(20.dp),
                            tint = colorResource(id = R.color.primary)
                        )
                    }
                }
            }

            // Search Dropdown Overlay - positioned below the app bar
            if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp)
                        .offset(y = 50.dp)
                        .zIndex(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                            4.dp
                        )
                    ) {
                        searchResults.take(5).forEach { product ->
                            Text(
                                text = product.name ?: "Unknown Product",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onProductClick(product)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontFamily = GeneralSans
                            )
                        }
                    }
                }
            }
        }
    }
}
