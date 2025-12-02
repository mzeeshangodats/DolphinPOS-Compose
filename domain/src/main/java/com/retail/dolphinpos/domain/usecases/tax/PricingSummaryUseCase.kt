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

        // Calculate cash tax from cart items (sum of cashTax * quantity)
        // Only include tax for items where chargeTaxOnThisProduct is true (not tax exempt)
        val cashTax = cartItems
            .filter { it.chargeTaxOnThisProduct == true }
            .sumOf { it.cashTax * it.quantity }

        // Calculate card subtotal (card prices with product discounts)
        val cardSubtotal = calculateSubtotalWithDiscounts(cartItems, useCashPrice = false)

        // Card discount is order discount
        val cardDiscount = orderDiscountTotal

        // Calculate card tax from cart items (sum of cardTax * quantity)
        // Only include tax for items where chargeTaxOnThisProduct is true (not tax exempt)
        val cardTax = cartItems
            .filter { it.chargeTaxOnThisProduct == true }
            .sumOf { it.cardTax * it.quantity }

        // Calculate correct totals: (subtotal - discount) + tax
        val cashTotal = (cashSubtotal - totalCashDiscount) + cashTax
        val cardTotal = (cardSubtotal - cardDiscount) + cardTax

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
     */
    private fun calculateSubtotalWithDiscounts(
        cartItems: List<CartItem>,
        useCashPrice: Boolean
    ): Double {
        return cartItems.sumOf { cartItem ->
            val basePrice = if (useCashPrice) cartItem.cashPrice else cartItem.cardPrice
            val discountedPrice = when (cartItem.discountType) {
                DiscountType.PERCENTAGE -> {
                    basePrice - ((basePrice * (cartItem.discountValue ?: 0.0)) / 100.0)
                }
                DiscountType.AMOUNT -> {
                    basePrice - (cartItem.discountValue ?: 0.0)
                }
                else -> basePrice
            }
            discountedPrice * cartItem.quantity
        }
    }
}

