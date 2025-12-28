package com.retail.dolphinpos.domain.usecases.refund

import com.retail.dolphinpos.domain.model.order.Order
import com.retail.dolphinpos.domain.model.refund.RefundRequest
import com.retail.dolphinpos.domain.model.refund.RefundedItem
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class CalculateRefundAmountUseCase @Inject constructor() {
    
    suspend fun calculateRefundAmount(request: RefundRequest, order: Order): Result<Pair<Double, List<RefundedItem>>> {
        return try {
            val orderItems = order.orderItems
            
            when (request.refundType) {
                com.retail.dolphinpos.domain.model.refund.RefundType.FULL -> {
                    calculateFullRefund(order, orderItems)
                }
                com.retail.dolphinpos.domain.model.refund.RefundType.PARTIAL -> {
                    if (request.customAmount != null) {
                        calculateCustomAmountRefund(order, orderItems, request.customAmount)
                    } else if (request.refundedItems != null && request.refundedItems.isNotEmpty()) {
                        calculatePartialRefund(order, orderItems, request.refundedItems)
                    } else {
                        Result.failure(IllegalArgumentException("Partial refund requires either refundedItems or customAmount"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun calculateFullRefund(
        order: Order,
        orderItems: List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>
    ): Result<Pair<Double, List<RefundedItem>>> {
        val refundedItems = orderItems.map { item ->
            RefundedItem(
                productId = item.productId,
                productVariantId = item.productVariantId,
                quantity = item.quantity ?: 0,
                amount = (item.price ?: 0.0) * (item.quantity ?: 0) - (item.discountedAmount ?: 0.0),
                originalPrice = item.price ?: 0.0,
                refundedPrice = (item.discountedPrice ?: item.price) ?: 0.0,
                itemDiscount = item.discountedAmount ?: 0.0,
                itemTax = item.totalTax ?: 0.0,
                orderDiscountProportion = 0.0 // Will be calculated proportionally
            )
        }
        
        // Calculate total refund amount including taxes and order-level discounts
        val itemsSubtotal = refundedItems.sumOf { it.amount }
        val itemsTax = refundedItems.sumOf { it.itemTax }
        val orderDiscount = order.discountAmount + order.cashDiscountAmount + order.rewardDiscount
        
        // Proportionally distribute order discount
        val orderTotal = order.subTotal
        val discountProportion = if (orderTotal > 0) orderDiscount / orderTotal else 0.0
        
        val refundedItemsWithOrderDiscount = refundedItems.map { item ->
            val itemOrderDiscount = item.amount * discountProportion
            item.copy(
                orderDiscountProportion = itemOrderDiscount,
                amount = item.amount - itemOrderDiscount + item.itemTax
            )
        }
        
        val totalRefundAmount = refundedItemsWithOrderDiscount.sumOf { it.amount }
        
        return Result.success(Pair(roundToTwoDecimals(totalRefundAmount), refundedItemsWithOrderDiscount))
    }
    
    private suspend fun calculatePartialRefund(
        order: Order,
        orderItems: List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>,
        refundedItemRequests: List<com.retail.dolphinpos.domain.model.refund.RefundedItemRequest>
    ): Result<Pair<Double, List<RefundedItem>>> {
        val refundedItems = mutableListOf<RefundedItem>()
        
        for (requestItem in refundedItemRequests) {
            val orderItem = orderItems.find { 
                it.productId == requestItem.productId && 
                it.productVariantId == requestItem.productVariantId 
            } ?: continue
            
            val originalQuantity = orderItem.quantity ?: 0
            val refundQuantity = requestItem.quantity
            
            if (refundQuantity > originalQuantity) {
                return Result.failure(IllegalArgumentException("Refund quantity ($refundQuantity) exceeds order quantity ($originalQuantity) for product ${orderItem.productId}"))
            }
            
            // Calculate proportional amounts
            val proportion = refundQuantity.toDouble() / originalQuantity.toDouble()
            val itemPrice = orderItem.price ?: 0.0
            val itemDiscountedPrice = orderItem.discountedPrice ?: itemPrice
            val itemDiscount = orderItem.discountedAmount ?: 0.0
            val itemTax = (orderItem.totalTax ?: 0.0) * proportion
            
            val refundedItem = RefundedItem(
                productId = requestItem.productId,
                productVariantId = requestItem.productVariantId,
                quantity = refundQuantity,
                amount = (itemDiscountedPrice * refundQuantity) + itemTax,
                originalPrice = itemPrice,
                refundedPrice = itemDiscountedPrice,
                itemDiscount = itemDiscount * proportion,
                itemTax = itemTax,
                orderDiscountProportion = 0.0 // Will be calculated below
            )
            
            refundedItems.add(refundedItem)
        }
        
        // Calculate order-level discount proportion
        val orderTotal = order.subTotal
        val orderDiscount = order.discountAmount + order.cashDiscountAmount + order.rewardDiscount
        val refundedSubtotal = refundedItems.sumOf { it.refundedPrice * it.quantity }
        val discountProportion = if (orderTotal > 0) (refundedSubtotal / orderTotal) * (orderDiscount / orderTotal) else 0.0
        
        val refundedItemsWithOrderDiscount = refundedItems.map { item ->
            val itemOrderDiscount = (item.refundedPrice * item.quantity) * discountProportion
            item.copy(
                orderDiscountProportion = itemOrderDiscount,
                amount = (item.refundedPrice * item.quantity) - itemOrderDiscount + item.itemTax
            )
        }
        
        val totalRefundAmount = refundedItemsWithOrderDiscount.sumOf { it.amount }
        
        return Result.success(Pair(roundToTwoDecimals(totalRefundAmount), refundedItemsWithOrderDiscount))
    }
    
    private suspend fun calculateCustomAmountRefund(
        order: Order,
        orderItems: List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>,
        customAmount: Double
    ): Result<Pair<Double, List<RefundedItem>>> {
        // For custom amount, we need to proportionally distribute across all items
        val orderTotal = order.total
        val proportion = if (orderTotal > 0) customAmount / orderTotal else 0.0
        
        val refundedItems = orderItems.map { item ->
            val itemTotal = ((item.discountedPrice ?: item.price) ?: 0.0) * (item.quantity ?: 0)
            val itemRefundAmount = itemTotal * proportion
            val itemTax = (item.totalTax ?: 0.0) * proportion
            
            RefundedItem(
                productId = item.productId,
                productVariantId = item.productVariantId,
                quantity = ((item.quantity ?: 0) * proportion).toInt(),
                amount = itemRefundAmount + itemTax,
                originalPrice = item.price ?: 0.0,
                refundedPrice = (item.discountedPrice ?: item.price) ?: 0.0,
                itemDiscount = (item.discountedAmount ?: 0.0) * proportion,
                itemTax = itemTax,
                orderDiscountProportion = 0.0
            )
        }
        
        return Result.success(Pair(roundToTwoDecimals(customAmount), refundedItems))
    }
    
    private fun roundToTwoDecimals(value: Double): Double {
        return BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}

