package com.retail.dolphinpos.presentation.features.ui.refund

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailList
import com.retail.dolphinpos.domain.model.home.order_details.OrderItem
import com.retail.dolphinpos.domain.model.refund.RefundType
import com.retail.dolphinpos.presentation.R
import java.util.Locale

@Composable
fun RefundDialog(
    order: OrderDetailList,
    onDismiss: () -> Unit,
    onRefund: (RefundType, List<RefundItemSelection>?, Double?) -> Unit
) {
    var refundType by remember { mutableStateOf<RefundType?>(null) }
    var selectedItems by remember { mutableStateOf<Map<Int, RefundItemSelection>>(emptyMap()) }
    var customAmount by remember { mutableStateOf("") }
    var showCustomAmountInput by remember { mutableStateOf(false) }
    
    val orderTotal = order.total.toDoubleOrNull() ?: 0.0
    val orderSubTotal = order.subTotal.toDoubleOrNull() ?: 0.0
    val orderDiscount = order.discountAmount.toDoubleOrNull() ?: 0.0
    val orderCashDiscount = when (val amount = order.cashDiscountAmount) {
        is String -> amount.toDoubleOrNull() ?: 0.0
        is Double -> amount
        is Int -> amount.toDouble()
        is Float -> amount.toDouble()
        is Long -> amount.toDouble()
        else -> 0.0
    }
    val orderRewardDiscount = when (val amount = order.rewardDiscount) {
        is String -> amount.toDoubleOrNull() ?: 0.0
        is Double -> amount
        is Int -> amount.toDouble()
        is Float -> amount.toDouble()
        is Long -> amount.toDouble()
        else -> 0.0
    }
    val orderTax = order.taxValue
    val totalOrderDiscount = orderDiscount + orderCashDiscount + orderRewardDiscount
    val refundedTotal = order.refundedTotal.toDoubleOrNull() ?: 0.0
    val remainingAmount = orderTotal - refundedTotal
    
    // Calculate refund amounts based on selected items
    val refundCalculations = remember(selectedItems, refundType, customAmount, order) {
        calculateRefundAmounts(
            order = order,
            selectedItems = selectedItems,
            refundType = refundType,
            customAmount = customAmount?.toDoubleOrNull(),
            orderSubTotal = orderSubTotal,
            totalOrderDiscount = totalOrderDiscount,
            orderTax = orderTax,
            orderTotal = orderTotal
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            BaseText(
                text = "Order Refund",
                fontSize = 18f,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = GeneralSans
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Order Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    BaseText(
                        text = "Order: ${order.orderNumber}",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BaseText(
                            text = "Total:",
                            fontSize = 14f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = String.format(Locale.US, "$%.2f", orderTotal),
                            fontSize = 14f,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontFamily = GeneralSans
                        )
                    }
                    if (refundedTotal > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BaseText(
                                text = "Refunded:",
                                fontSize = 14f,
                                color = Color.Gray,
                                fontFamily = GeneralSans
                            )
                            BaseText(
                                text = String.format(Locale.US, "$%.2f", refundedTotal),
                                fontSize = 14f,
                                fontWeight = FontWeight.SemiBold,
                                color = colorResource(R.color.red_error),
                                fontFamily = GeneralSans
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BaseText(
                                text = "Remaining:",
                                fontSize = 14f,
                                color = Color.Gray,
                                fontFamily = GeneralSans
                            )
                            BaseText(
                                text = String.format(Locale.US, "$%.2f", remainingAmount),
                                fontSize = 14f,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black,
                                fontFamily = GeneralSans
                            )
                        }
                    }
                }
                
                // Refund Type Selection
                BaseText(
                    text = "Refund Type:",
                    fontSize = 14f,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    fontFamily = GeneralSans
                )
                
                // Full Refund Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            refundType = RefundType.FULL
                            selectedItems = emptyMap()
                            showCustomAmountInput = false
                            customAmount = ""
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = refundType == RefundType.FULL,
                        onClick = { 
                            refundType = RefundType.FULL
                            selectedItems = emptyMap()
                            showCustomAmountInput = false
                            customAmount = ""
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        BaseText(
                            text = "Full Refund",
                            fontSize = 14f,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = "Refund entire order amount",
                            fontSize = 12f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                    }
                }
                
                // Partial Refund Option - Item Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            refundType = RefundType.PARTIAL
                            showCustomAmountInput = false
                            customAmount = ""
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = refundType == RefundType.PARTIAL && !showCustomAmountInput,
                        onClick = { 
                            refundType = RefundType.PARTIAL
                            showCustomAmountInput = false
                            customAmount = ""
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        BaseText(
                            text = "Partial Refund - Select Items",
                            fontSize = 14f,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = "Select specific items to refund",
                            fontSize = 12f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                    }
                }
                
                // Partial Refund Option - Custom Amount
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            refundType = RefundType.PARTIAL
                            showCustomAmountInput = true
                            selectedItems = emptyMap()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = refundType == RefundType.PARTIAL && showCustomAmountInput,
                        onClick = { 
                            refundType = RefundType.PARTIAL
                            showCustomAmountInput = true
                            selectedItems = emptyMap()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        BaseText(
                            text = "Partial Refund - Custom Amount",
                            fontSize = 14f,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            fontFamily = GeneralSans
                        )
                        BaseText(
                            text = "Enter custom refund amount",
                            fontSize = 12f,
                            color = Color.Gray,
                            fontFamily = GeneralSans
                        )
                    }
                }
                
                // Custom Amount Input
                if (refundType == RefundType.PARTIAL && showCustomAmountInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        BaseText(
                            text = "Refund Amount:",
                            fontSize = 14f,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            fontFamily = GeneralSans,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = customAmount,
                                onValueChange = { newValue ->
                                    // Allow only numbers and one decimal point
                                    if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        val amount = newValue.toDoubleOrNull() ?: 0.0
                                        if (amount <= remainingAmount) {
                                            customAmount = newValue
                                        }
                                    }
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    fontFamily = GeneralSans,
                                    color = Color.Black
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (customAmount.isEmpty()) {
                                BaseText(
                                    text = "Enter amount (max: $${String.format(Locale.US, "%.2f", remainingAmount)})",
                                    fontSize = 14f,
                                    color = Color.Gray,
                                    fontFamily = GeneralSans
                                )
                            }
                        }
                    }
                }
                
                // Item Selection for Partial Refund
                if (refundType == RefundType.PARTIAL && !showCustomAmountInput) {
                    Spacer(modifier = Modifier.height(4.dp))
                    BaseText(
                        text = "Select Items to Refund:",
                        fontSize = 14f,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        fontFamily = GeneralSans
                    )
                    
                    order.orderItems.forEachIndexed { index, item ->
                        val itemKey = index
                        val isSelected = selectedItems.containsKey(itemKey)
                        val selection = selectedItems[itemKey]
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) Color(0xFFE3F2FD) else Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) colorResource(R.color.primary) else Color.LightGray,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedItems = selectedItems + (itemKey to RefundItemSelection(
                                            productId = item.product.id,
                                            productVariantId = if (item.productVariant is Int) item.productVariant as Int else null,
                                            quantity = item.quantity
                                        ))
                                    } else {
                                        selectedItems = selectedItems - itemKey
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                BaseText(
                                    text = item.product.name,
                                    fontSize = 14f,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black,
                                    fontFamily = GeneralSans
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    BaseText(
                                        text = "Qty: ${item.quantity}",
                                        fontSize = 12f,
                                        color = Color.Gray,
                                        fontFamily = GeneralSans
                                    )
                                    BaseText(
                                        text = String.format(
                                            Locale.US,
                                            "$%.2f",
                                            (item.price.toDoubleOrNull() ?: 0.0) * item.quantity
                                        ),
                                        fontSize = 12f,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black,
                                        fontFamily = GeneralSans
                                    )
                                }
                                if (isSelected && selection != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    // Quantity selector for selected item
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        BaseText(
                                            text = "Refund Qty:",
                                            fontSize = 12f,
                                            color = Color.Gray,
                                            fontFamily = GeneralSans
                                        )
                                        BaseButton(
                                            text = "-",
                                            modifier = Modifier.size(32.dp),
                                            backgroundColor = Color.White,
                                            textColor = colorResource(R.color.primary),
                                            fontSize = 18,
                                            border = BorderStroke(1.dp, colorResource(R.color.primary)),
                                            contentPadding = PaddingValues(0.dp),
                                            onClick = {
                                                if (selection.quantity > 1) {
                                                    selectedItems = selectedItems + (itemKey to selection.copy(
                                                        quantity = selection.quantity - 1
                                                    ))
                                                }
                                            }
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(40.dp)
                                                .height(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            BaseText(
                                                text = "${selection.quantity}",
                                                fontSize = 14f,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.Black,
                                                fontFamily = GeneralSans,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        BaseButton(
                                            text = "+",
                                            modifier = Modifier.size(32.dp),
                                            backgroundColor = Color.White,
                                            textColor = colorResource(R.color.primary),
                                            fontSize = 18,
                                            border = BorderStroke(1.dp, colorResource(R.color.primary)),
                                            contentPadding = PaddingValues(0.dp),
                                            onClick = {
                                                if (selection.quantity < item.quantity) {
                                                    selectedItems = selectedItems + (itemKey to selection.copy(
                                                        quantity = selection.quantity + 1
                                                    ))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
//                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                
                // Refund Summary (only show if refund type is selected)
                if (refundType != null && (refundType == RefundType.FULL || selectedItems.isNotEmpty() || (customAmount.isNotEmpty() && customAmount.toDoubleOrNull() != null))) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        BaseText(
                            text = "Refund Summary",
                            fontSize = 14f,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            fontFamily = GeneralSans,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Refund Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BaseText(
                                text = "Refund Subtotal:",
                                fontSize = 14f,
                                color = Color.Gray,
                                fontFamily = GeneralSans
                            )
                            BaseText(
                                text = String.format(Locale.US, "$%.2f", refundCalculations.refundSubTotal),
                                fontSize = 14f,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black,
                                fontFamily = GeneralSans
                            )
                        }
                        
                        // Refund Discount
                        if (refundCalculations.refundDiscount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BaseText(
                                    text = "Refund Discount:",
                                    fontSize = 14f,
                                    color = Color.Gray,
                                    fontFamily = GeneralSans
                                )
                                BaseText(
                                    text = String.format(Locale.US, "-$%.2f", refundCalculations.refundDiscount),
                                    fontSize = 14f,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorResource(R.color.green_success),
                                    fontFamily = GeneralSans
                                )
                            }
                        }
                        
                        // Refund Tax
                        if (refundCalculations.refundTax > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BaseText(
                                    text = "Refund Tax:",
                                    fontSize = 14f,
                                    color = Color.Gray,
                                    fontFamily = GeneralSans
                                )
                                BaseText(
                                    text = String.format(Locale.US, "$%.2f", refundCalculations.refundTax),
                                    fontSize = 14f,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black,
                                    fontFamily = GeneralSans
                                )
                            }
                        }
                        
                        // Refund Total
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BaseText(
                                text = "Refund Total:",
                                fontSize = 16f,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontFamily = GeneralSans
                            )
                            BaseText(
                                text = String.format(Locale.US, "$%.2f", refundCalculations.refundTotal),
                                fontSize = 16f,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.red_error),
                                fontFamily = GeneralSans
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BaseButton(
                    text = "Cancel",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color.Gray,
                    textColor = Color.White,
                    fontSize = 14,
                    fontWeight = FontWeight.SemiBold,
                    height = 40.dp,
                    onClick = onDismiss
                )
                BaseButton(
                    text = "Order Refund",
                    modifier = Modifier.weight(1f),
                    backgroundColor = colorResource(R.color.red_error),
                    textColor = Color.White,
                    fontSize = 14,
                    fontWeight = FontWeight.SemiBold,
                    height = 40.dp,
                    onClick = {
                        if (refundType != null) {
                            when (refundType) {
                                RefundType.FULL -> {
                                    onRefund(RefundType.FULL, null, null)
                                }
                                RefundType.PARTIAL -> {
                                    if (showCustomAmountInput) {
                                        val amount = customAmount.toDoubleOrNull()
                                        if (amount != null && amount > 0 && amount <= remainingAmount) {
                                            onRefund(RefundType.PARTIAL, null, amount)
                                        }
                                    } else {
                                        if (selectedItems.isNotEmpty()) {
                                            onRefund(RefundType.PARTIAL, selectedItems.values.toList(), null)
                                        }
                                    }
                                }
                                null -> {}
                            }
                        }
                    }
                )
            }
        }
    )
}

data class RefundItemSelection(
    val productId: Int,
    val productVariantId: Int?,
    val quantity: Int
)

data class RefundCalculations(
    val refundSubTotal: Double = 0.0,
    val refundDiscount: Double = 0.0,
    val refundTax: Double = 0.0,
    val refundTotal: Double = 0.0
)

@Composable
fun calculateRefundAmounts(
    order: OrderDetailList,
    selectedItems: Map<Int, RefundItemSelection>,
    refundType: RefundType?,
    customAmount: Double?,
    orderSubTotal: Double,
    totalOrderDiscount: Double,
    orderTax: Double,
    orderTotal: Double
): RefundCalculations {
    return when (refundType) {
        RefundType.FULL -> {
            // Full refund - return all order amounts
            RefundCalculations(
                refundSubTotal = orderSubTotal,
                refundDiscount = totalOrderDiscount,
                refundTax = orderTax,
                refundTotal = orderTotal
            )
        }
        RefundType.PARTIAL -> {
            if (customAmount != null && customAmount > 0) {
                // Custom amount refund - proportionally distribute
                val proportion = if (orderTotal > 0) customAmount / orderTotal else 0.0
                RefundCalculations(
                    refundSubTotal = orderSubTotal * proportion,
                    refundDiscount = totalOrderDiscount * proportion,
                    refundTax = orderTax * proportion,
                    refundTotal = customAmount
                )
            } else if (selectedItems.isNotEmpty()) {
                // Partial refund with selected items
                var refundSubTotal = 0.0
                var refundItemDiscount = 0.0
                var refundItemTax = 0.0
                
                order.orderItems.forEachIndexed { index, item ->
                    val selection = selectedItems[index] ?: return@forEachIndexed
                    val originalQuantity = item.quantity
                    val refundQuantity = selection.quantity
                    val proportion = if (originalQuantity > 0) refundQuantity.toDouble() / originalQuantity.toDouble() else 0.0
                    
                    val itemPrice = item.price.toDoubleOrNull() ?: 0.0
                    val itemDiscountedPrice = item.discountedPrice.toDoubleOrNull() ?: itemPrice
                    val itemTotal = itemDiscountedPrice * originalQuantity
                    val itemDiscount = if (item.isDiscounted) {
                        (itemPrice - itemDiscountedPrice) * originalQuantity
                    } else {
                        0.0
                    }
                    
                    // Calculate proportional values
                    refundSubTotal += itemDiscountedPrice * refundQuantity
                    refundItemDiscount += itemDiscount * proportion
                    
                    // Calculate tax proportionally based on item value
                    // Tax is typically calculated on subtotal, so we distribute it proportionally
                    if (orderSubTotal > 0) {
                        val itemValue = itemDiscountedPrice * refundQuantity
                        val itemTaxProportion = itemValue / orderSubTotal
                        refundItemTax += orderTax * itemTaxProportion
                    }
                }
                
                // Calculate order-level discount proportion
                val orderDiscountProportion = if (orderSubTotal > 0) refundSubTotal / orderSubTotal else 0.0
                val refundOrderDiscount = totalOrderDiscount * orderDiscountProportion
                
                val totalRefundDiscount = refundItemDiscount + refundOrderDiscount
                val refundTotal = refundSubTotal - totalRefundDiscount + refundItemTax
                
                RefundCalculations(
                    refundSubTotal = refundSubTotal,
                    refundDiscount = totalRefundDiscount,
                    refundTax = refundItemTax,
                    refundTotal = refundTotal
                )
            } else {
                RefundCalculations()
            }
        }
        null -> RefundCalculations()
    }
}

