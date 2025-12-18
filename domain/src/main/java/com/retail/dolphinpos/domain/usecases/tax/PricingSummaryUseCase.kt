package com.retail.dolphinpos.domain.usecases.tax

import com.retail.dolphinpos.domain.model.home.cart.CartItem
import com.retail.dolphinpos.domain.model.home.cart.DiscountType
import javax.inject.Inject

data class PricingSummaryResult(
    val cashSubtotal: Double,
    val cashOrderDiscount: Double,
    val totalCashDiscount: Double,
    val cashTax: Double,
    val cashTotal: Double,
    val cardSubtotal: Double,
    val cardDiscount: Double,
    val cardTax: Double,
    val cardTotal: Double
)

class PricingSummaryUseCase @Inject constructor() {

    /**
     * Calculate pricing summary for both cash and card price cards
     */
    fun calculatePricingSummary(
        cartItems: List<CartItem>,
        subtotal: Double,
        cashDiscountTotal: Double,
        orderDiscountTotal: Double,
        isCashSelected: Boolean
    ): PricingSummaryResult {
        // Calculate cash subtotal (cash prices with product discounts)
        val cashSubtotal = calculateSubtotalWithDiscounts(cartItems, useCashPrice = true)

        // Calculate order discount for cash (proportional to cash subtotal)
        val cashOrderDiscount = if (subtotal > 0 && orderDiscountTotal > 0) {
            // Apply the same percentage discount to cash subtotal
            val discountPercentage = orderDiscountTotal / subtotal
            cashSubtotal * discountPercentage
        } else {
            0.0
        }

        // Calculate total cash discount
        // When cash is selected, don't show cash discount (only show order discount)
        // When card is selected, show both cash discount and order discount
        val totalCashDiscount = if (isCashSelected) {
            // Only show order discount when cash is selected
            cashOrderDiscount
        } else {
            // Show both cash discount and order discount when card is selected
            cashDiscountTotal + cashOrderDiscount
        }

        // Calculate cash discounted subtotal (after order-level discounts and cash discount)
        // Cap at 0.0 to prevent negative subtotals
        val cashDiscountedSubtotal = (cashSubtotal - totalCashDiscount).coerceAtLeast(0.0)

        // Calculate cash tax on the discounted subtotal (not on original subtotal)
        // Rule: Tax must be calculated on the discounted subtotal after order-level discounts
        // First, calculate the effective tax rate from original cash subtotal and tax
        val originalCashTax = cartItems
            .filter { it.chargeTaxOnThisProduct == true }
            .sumOf { it.cashTax * it.quantity }
        val cashTaxRate = if (cashSubtotal > 0) {
            originalCashTax / cashSubtotal
        } else {
            0.0
        }
        // Apply tax rate to discounted subtotal (if discounted subtotal is 0, tax is 0)
        val cashTax = if (cashDiscountedSubtotal <= 0) {
            0.0
        } else {
            cashDiscountedSubtotal * cashTaxRate
        }

        // Calculate card subtotal (card prices with product discounts)
        val cardSubtotal = calculateSubtotalWithDiscounts(cartItems, useCashPrice = false)

        // Card discount is order discount
        val cardDiscount = orderDiscountTotal

        // Calculate card discounted subtotal (after order-level discounts)
        // Cap at 0.0 to prevent negative subtotals
        val cardDiscountedSubtotal = (cardSubtotal - cardDiscount).coerceAtLeast(0.0)

        // Calculate card tax on the discounted subtotal (not on original subtotal)
        // Rule: Tax must be calculated on the discounted subtotal after order-level discounts
        // First, calculate the effective tax rate from original card subtotal and tax
        val originalCardTax = cartItems
            .filter { it.chargeTaxOnThisProduct == true }
            .sumOf { it.cardTax * it.quantity }
        val cardTaxRate = if (cardSubtotal > 0) {
            originalCardTax / cardSubtotal
        } else {
            0.0
        }
        // Apply tax rate to discounted subtotal (if discounted subtotal is 0, tax is 0)
        val cardTax = if (cardDiscountedSubtotal <= 0) {
            0.0
        } else {
            cardDiscountedSubtotal * cardTaxRate
        }

        // Calculate correct totals: (subtotal - discount) + tax
        // Use capped discounted subtotals to ensure totals don't go negative
        val cashTotal = cashDiscountedSubtotal + cashTax
        val cardTotal = cardDiscountedSubtotal + cardTax

        return PricingSummaryResult(
            cashSubtotal = cashSubtotal,
            cashOrderDiscount = cashOrderDiscount,
            totalCashDiscount = totalCashDiscount,
            cashTax = cashTax,
            cashTotal = cashTotal,
            cardSubtotal = cardSubtotal,
            cardDiscount = cardDiscount,
            cardTax = cardTax,
            cardTotal = cardTotal
        )
    }

    /**
     * Calculate subtotal with product-level discounts applied
     * Caps discounted price at 0.0 to prevent negative subtotals
     */
    private fun calculateSubtotalWithDiscounts(
        cartItems: List<CartItem>,
        useCashPrice: Boolean
    ): Double {
        return cartItems.sumOf { cartItem ->
            val basePrice = if (useCashPrice) cartItem.cashPrice else cartItem.cardPrice
            val discountedPrice = when (cartItem.discountType) {
                DiscountType.PERCENTAGE -> {
                    (basePrice - ((basePrice * (cartItem.discountValue ?: 0.0)) / 100.0)).coerceAtLeast(0.0)
                }
                DiscountType.AMOUNT -> {
                    (basePrice - (cartItem.discountValue ?: 0.0)).coerceAtLeast(0.0)
                }
                else -> basePrice
            }
            discountedPrice * cartItem.quantity
        }
    }
}

