package com.retail.dolphinpos.presentation.features.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.data.entities.holdcart.HoldCartEntity
import com.retail.dolphinpos.presentation.R

@Composable
fun HoldCartListDialog(
    onDismiss: () -> Unit,
    onRestoreCart: (Long) -> Unit,
    onDeleteCart: (Long) -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val holdCarts by viewModel.holdCarts.collectAsStateWithLifecycle()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Hold Carts",
                fontFamily = GeneralSans,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            if (holdCarts.isEmpty()) {
                Text(
                    text = "No hold carts available",
                    fontFamily = GeneralSans,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(holdCarts) { holdCart ->
                        HoldCartItem(
                            holdCart = holdCart,
                            onRestore = { onRestoreCart(holdCart.id) },
                            onDelete = { onDeleteCart(holdCart.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary)
                )
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun HoldCartItem(
    holdCart: HoldCartEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Guest Cart",
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Text(
                    text = "Total: $${String.format("%.2f", holdCart.totalAmount)}",
                    fontFamily = GeneralSans,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Created: ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(holdCart.createdAt))}",
                    fontFamily = GeneralSans,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.green_success)
                    ),
                    modifier = Modifier.size(100.dp, 32.dp),
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Restore",
                        fontSize = 10.sp,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    modifier = Modifier.size(100.dp, 32.dp),
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Delete",
                        fontSize = 10.sp,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
