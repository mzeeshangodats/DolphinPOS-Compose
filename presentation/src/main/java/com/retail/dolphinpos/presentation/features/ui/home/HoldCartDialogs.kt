package com.retail.dolphinpos.presentation.features.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.data.entities.holdcart.HoldCartEntity
import com.retail.dolphinpos.presentation.R
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HoldCartListDialog(
    onDismiss: () -> Unit,
    onRestoreCart: (Long) -> Unit,
    onDeleteCart: (Long) -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val holdCarts by viewModel.holdCarts.collectAsStateWithLifecycle()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(600.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BaseText(
                        text = "Saved Carts",
                        color = Color.Black,
                        fontSize = 20f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        BaseText(
                            text = "✕",
                            color = Color.Black,
                            fontSize = 18f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Blue Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = colorResource(id = R.color.primary),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BaseText(
                        text = "Customer",
                        color = Color.White,
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    BaseText(
                        text = "Amount",
                        color = Color.White,
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    BaseText(
                        text = "Date",
                        color = Color.White,
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    BaseText(
                        text = "Actions",
                        color = Color.White,
                        fontSize = 14f,
                        fontFamily = GeneralSans,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Cart Items List
                if (holdCarts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = "No hold carts available",
                            color = Color.Gray,
                            fontSize = 14f,
                            fontFamily = GeneralSans
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(holdCarts) { holdCart ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorResource(id = R.color.light_grey)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = BorderStroke(1.dp, colorResource(id = R.color.borderOutline))
                            ) {
                                HoldCartItem(
                                    holdCart = holdCart,
                                    onRestore = { onRestoreCart(holdCart.id) },
                                    onDelete = { onDeleteCart(holdCart.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HoldCartItem(
    holdCart: HoldCartEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    // Format date like "Monday, Jun 17, 2024 2:32 PM"
    val formattedDate = remember(holdCart.createdAt) {
        try {
            val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy h:mm a", Locale.getDefault())
            dateFormat.format(java.util.Date(holdCart.createdAt))
        } catch (e: Exception) {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(java.util.Date(holdCart.createdAt))
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Customer Column
        BaseText(
            text = holdCart.cartName,
            color = Color.Black,
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        
        // Amount Column
        BaseText(
            text = "$${String.format(Locale.US, "%.2f", holdCart.totalAmount)}",
            color = Color.Black,
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        // Date Column
        BaseText(
            text = formattedDate,
            color = Color.Black,
            fontSize = 12f,
            fontFamily = GeneralSans,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        
        // Actions Column
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    // Delete Button (Blue with trash icon)

                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete_1),
                            contentDescription = "Delete",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(50.dp).clickable { onDelete() }
                        )

                    
                    // Restore Button (Green with circular arrow icon)

                        Icon(
                            painter = painterResource(id = R.drawable.ic_restore),
                            contentDescription = "Restore",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(50.dp).clickable { onRestore() }
                        )

                }

        }
    }
}
