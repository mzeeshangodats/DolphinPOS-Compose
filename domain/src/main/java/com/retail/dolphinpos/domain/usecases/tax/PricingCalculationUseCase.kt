package com.retail.dolphinpos.domain.usecases.tax

import android.util.Log
import com.retail.dolphinpos.domain.model.TaxDetail
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import com.retail.dolphinpos.domain.model.home.cart.applyTax
import com.retail.dolphinpos.domain.model.home.cart.getProductDiscountedPrice
import com.retail.dolphinpos.domain.model.home.cart.price
import javax.inject.Inject
import kotlin.math.round

data class PricingResult(
    val subtotal: Double,
    val productLevelDiscount: Double,
    val orderLevelDiscount: Double,
    val rewardDiscount: Double,
    val subtotalAfterDiscounts: Double,
    val tax: Double,
    val total: Double,
    val isTaxApplied: Boolean,
    // Product-level tax breakdown
    val cartItemsWithTax: List<CartItem> = emptyList(),
    val totalProductTaxAmount: Double = 0.0
)

data class PricingConfiguration(
    val isTaxApplied: Boolean = true,
    val taxRate: Double = 0.10, // 10% default - kept for backward compatibility
    val useCardPricing: Boolean = true,
    // Complex tax structure
    val taxDetails: List<TaxDetail>? = null,
    val taxExempt: Boolean = false
)

class PricingCalculationUseCase @Inject constructor() {

    /**
     * Calculate pricing for cart items with support for multiple price types and layered discounts
     */
    fun calculatePricing(
        cartItems: List<CartItem>,
        discountOrder: DiscountOrder? = null,
        rewardAmount: Double = 0.0,
        config: PricingConfiguration = PricingConfiguration()
    ): PricingResult {

        // Step 1: Calculate base subtotal using appropriate price type
        val subtotal = calculateSubtotal(cartItems, config.useCardPricing)

        // Step 2: Calculate product-level discounts (not shown to user)
        val productLevelDiscount = calculateProductLevelDiscounts(cartItems, config.useCardPricing)

        // Step 3: Calculate order-level discount (shown to user) - apply on subtotal after product and reward discounts
        val subtotalAfterProductAndRewardDiscounts = subtotal - productLevelDiscount - rewardAmount
        val orderLevelDiscount = calculateOrderLevelDiscount(discountOrder, subtotalAfterProductAndRewardDiscounts)

        // Step 4: Calculate subtotal after all discounts
        val subtotalAfterDiscounts = subtotalAfterProductAndRewardDiscounts - orderLevelDiscount

        // Step 5: Calculate product-level tax (only if isTaxApplied is true and not tax-exempt)
        val (cartItemsWithTax, totalProductTaxAmount) = if (config.isTaxApplied && !config.taxExempt) {
            calculateProductLevelTax(
                cartItems = cartItems,
                taxRate = config.taxRate,
                useCardPricing = config.useCardPricing,
                subtotalAfterDiscounts = subtotalAfterDiscounts,
                originalSubtotal = subtotal,
                taxDetails = config.taxDetails,
                taxExempt = config.taxExempt
            )
        } else {
            // No tax applied or tax-exempt store
            Log.d("PricingCalculation", "Tax not applied - isTaxApplied: ${config.isTaxApplied}, taxExempt: ${config.taxExempt}")
            Pair(cartItems.map { it.copy(
                productTaxAmount = 0.0,
                productTaxRate = 0.0,
                productTaxableAmount = 0.0
            )}, 0.0)
        }

        Log.d("PricingCalculation", "Total product tax amount: $totalProductTaxAmount")

        // Step 6: Calculate final total
        val total = subtotalAfterDiscounts + totalProductTaxAmount

        return PricingResult(
            subtotal = subtotal,
            productLevelDiscount = productLevelDiscount,
            orderLevelDiscount = orderLevelDiscount,
            rewardDiscount = rewardAmount,
            subtotalAfterDiscounts = subtotalAfterDiscounts,
            tax = totalProductTaxAmount, // Use product-level tax total
            total = total,
            isTaxApplied = config.isTaxApplied,
            cartItemsWithTax = cartItemsWithTax,
            totalProductTaxAmount = totalProductTaxAmount
        )
    }

    /**
     * Calculate product-level tax with support for complex tax structures and edge cases
     */
    private fun calculateProductLevelTax(
        cartItems: List<CartItem>,
        taxRate: Double,
        useCardPricing: Boolean,
        subtotalAfterDiscounts: Double,
        originalSubtotal: Double,
        taxDetails: List<TaxDetail>? = null,
        taxExempt: Boolean = false
    ): Pair<List<CartItem>, Double> {

        Log.d("PricingCalculation", "=== calculateProductLevelTax START ===")
        Log.d("PricingCalculation", "Cart items count: ${cartItems.size}")
        Log.d("PricingCalculation", "TaxRate: $taxRate")
        Log.d("PricingCalculation", "UseCardPricing: $useCardPricing")
        Log.d("PricingCalculation", "SubtotalAfterDiscounts: $subtotalAfterDiscounts")
        Log.d("PricingCalculation", "OriginalSubtotal: $originalSubtotal")
        Log.d("PricingCalculation", "TaxDetails: ${taxDetails?.size ?: "null"}")
        Log.d("PricingCalculation", "TaxExempt: $taxExempt")

        // Edge case: Empty cart
        if (cartItems.isEmpty()) {
            Log.d("PricingCalculation", "Edge case: Empty cart - returning empty list")
            return Pair(emptyList(), 0.0)
        }

        // Edge case: Zero subtotal
        if (originalSubtotal <= 0.0) {
            return Pair(cartItems.map { it.copy(
                productTaxAmount = 0.0,
                productTaxRate = 0.0,
                productTaxableAmount = 0.0
            )}, 0.0)
        }

        val updatedCartItems = cartItems.map { item ->
            // Edge case: Item with zero quantity
            if ((item.quantity ?: 0) <= 0) {
                return@map item.copy(
                    productTaxAmount = 0.0,
                    productTaxRate = 0.0,
                    productTaxableAmount = 0.0
                )
            }

            // Edge case: Non-taxable item or tax-exempt store
            if (!item.applyTax || taxExempt) {
                Log.d("PricingCalculation", "Product ${item.name} is tax-exempt - applyTax: ${item.applyTax}, storeTaxExempt: $taxExempt")
                return@map item.copy(
                    productTaxAmount = 0.0,
                    productTaxRate = 0.0,
                    productTaxableAmount = 0.0
                )
            }

            // Calculate item price based on pricing type
            // Note: CartItem.price already contains the card price (base price + dual price percentage)
            // Check for product-level discount using discountType and discountValue
            val hasProductDiscount = item.discountType != null && item.discountValue != null && item.discountValue!! > 0.0
            
            val itemPrice = when {
                useCardPricing -> {
                    when {
                        hasProductDiscount -> {
                            // Use getProductDiscountedPrice() for product-level discounts
                            item.getProductDiscountedPrice()
                        }
                        item.isDiscounted && item.discountPrice != null -> {
                            item.discountPrice!!  // This is already cardDiscountedPrice
                        }
                        else -> {
                            item.price ?: 0.0    // This is already cardPrice
                        }
                    }
                }
                else -> {
                    when {
                        hasProductDiscount -> {
                            // For cash pricing, calculate discounted price from cashPrice
                            val cashPrice = item.cashPrice
                            when (item.discountType) {
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE -> {
                                    cashPrice - ((cashPrice * (item.discountValue ?: 0.0)) / 100.0)
                                }
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT -> {
                                    cashPrice - (item.discountValue ?: 0.0)
                                }
                                else -> cashPrice
                            }
                        }
                        item.isDiscounted && item.cashDiscountedPrice > 0.0 -> {
                            item.cashDiscountedPrice
                        }
                        else -> {
                            item.cashPrice
                        }
                    }
                }
            }

            // Edge case: Item with zero price
            if (itemPrice <= 0.0) {
                return@map item.copy(
                    productTaxAmount = 0.0,
                    productTaxRate = 0.0,
                    productTaxableAmount = 0.0
                )
            }

            val itemSubtotal = itemPrice * (item.quantity ?: 1)

            // Calculate the proportion of this item's value to total subtotal
            val itemProportion = if (originalSubtotal > 0.0) itemSubtotal / originalSubtotal else 0.0

            // Apply the same proportion to the discounted subtotal
            val itemTaxableAmount = subtotalAfterDiscounts * itemProportion

            // Calculate tax for this specific item with edge case handling
            // Use cardTax/cashTax from Products if available, otherwise calculate from tax details
            val (itemTaxAmount, totalTaxRate) = if (item.chargeTaxOnThisProduct == true) {
                // Use cardTax or cashTax directly from Products (multiplied by quantity)
                val taxFromProduct = if (useCardPricing) {
                    item.cardTax * (item.quantity ?: 1)
                } else {
                    item.cashTax * (item.quantity ?: 1)
                }
                
                // Calculate tax rate from the tax amount
                val calculatedRate = if (itemTaxableAmount > 0) {
                    taxFromProduct / itemTaxableAmount
                } else {
                    taxRate // Fallback to default rate
                }
                
                Pair(taxFromProduct, calculatedRate)
            } else {
                // Product is tax exempt, return zero tax
                Pair(0.0, 0.0)
            }

            item.copy(
                productTaxAmount = itemTaxAmount,
                productTaxRate = totalTaxRate,
                productTaxableAmount = itemTaxableAmount
            )
        }

        val totalTaxAmount = updatedCartItems.sumOf { it.productTaxAmount }

        Log.d("PricingCalculation", "Total product tax amount: $totalTaxAmount")
        Log.d("PricingCalculation", "=== calculateProductLevelTax END ===")

        return Pair(updatedCartItems, totalTaxAmount)
    }

    /**
     * Calculate tax for a specific item with comprehensive edge case handling
     * Combines store taxes (default) + product taxes (non-default)
     */
    private fun calculateItemTax(
        item: CartItem,
        itemTaxableAmount: Double,
        taxDetails: List<TaxDetail>?,
        fallbackTaxRate: Double
    ): Pair<Double, Double> {

        Log.d("PricingCalculation", "=== calculateItemTax START ===")
        Log.d("PricingCalculation", "Item: ${item.name}")
        Log.d("PricingCalculation", "ItemTaxableAmount: $itemTaxableAmount")
        Log.d("PricingCalculation", "FallbackTaxRate: $fallbackTaxRate")

        // Edge case: Zero taxable amount
        if (itemTaxableAmount <= 0.0) {
            Log.d("PricingCalculation", "Edge case: Zero taxable amount - returning (0.0, 0.0)")
            return Pair(0.0, 0.0)
        }

        // Edge case: Product is tax-exempt
        if (!item.applyTax) {
            Log.d("PricingCalculation", "Edge case: Product is tax-exempt (applyTax: false) - returning (0.0, 0.0)")
            return Pair(0.0, 0.0)
        }

        var totalTaxAmount = 0.0
        var totalTaxRate = 0.0

        // Check product tax details to determine tax calculation strategy
        val productTaxDetails = item.productTaxDetails
        Log.d("PricingCalculation", "ProductTaxDetails: ${productTaxDetails?.size ?: "null"}")
        productTaxDetails?.forEach { tax ->
            Log.d("PricingCalculation", "  - ${tax.title}: ${tax.value}% (isDefault: ${tax.isDefault})")
        }

        when {
            // Case 1: Product has taxDetails with values (not empty/null) - Apply BOTH store + product taxes
            productTaxDetails != null && productTaxDetails.isNotEmpty() -> {
                Log.d("PricingCalculation", "CASE 1: Product HAS additional taxes - applying store taxes (default) + product taxes (non-default)")

                // Apply store taxes (default taxes only)
                if (taxDetails != null && taxDetails.isNotEmpty()) {
                    Log.d("PricingCalculation", "Applying store taxes (default taxes)...")
                    val (storeTaxAmount, storeTaxRate) = calculateComplexTax(itemTaxableAmount, taxDetails, isStoreTax = true)
                    totalTaxAmount += storeTaxAmount
                    totalTaxRate += storeTaxRate
                    Log.d("PricingCalculation", "Store taxes applied: Amount=$storeTaxAmount, Rate=$storeTaxRate")
                } else {
                    Log.d("PricingCalculation", "No store taxes available")
                }

                // Apply product taxes (non-default taxes)
                Log.d("PricingCalculation", "Applying product taxes (non-default taxes)...")
                val (productTaxAmount, productTaxRate) = calculateComplexTax(itemTaxableAmount, productTaxDetails, isStoreTax = false)
                totalTaxAmount += productTaxAmount
                totalTaxRate += productTaxRate
                Log.d("PricingCalculation", "Product taxes applied: Amount=$productTaxAmount, Rate=$productTaxRate")
            }

            // Case 2: Product has taxDetails empty array or null - Apply ONLY store taxes
            else -> {
                Log.d("PricingCalculation", "CASE 2: Product HAS NO additional taxes - applying ONLY store taxes (default)")

                // Apply ONLY store taxes (default taxes)
                if (taxDetails != null && taxDetails.isNotEmpty()) {
                    Log.d("PricingCalculation", "Applying store taxes (default taxes) only...")
                    val (storeTaxAmount, storeTaxRate) = calculateComplexTax(itemTaxableAmount, taxDetails, isStoreTax = true)
                    totalTaxAmount += storeTaxAmount
                    totalTaxRate += storeTaxRate
                    Log.d("PricingCalculation", "Store taxes applied: Amount=$storeTaxAmount, Rate=$storeTaxRate")
                } else {
                    Log.d("PricingCalculation", "No store taxes available, using fallback tax rate...")
                    // Fallback to simple tax rate if no store tax details
                    val simpleTaxAmount = itemTaxableAmount * fallbackTaxRate
                    totalTaxAmount = simpleTaxAmount
                    totalTaxRate = fallbackTaxRate
                    Log.d("PricingCalculation", "Fallback tax applied: Amount=$simpleTaxAmount, Rate=$fallbackTaxRate")
                }
            }
        }

        Log.d("PricingCalculation", "Final result: TotalTaxAmount=$totalTaxAmount, TotalTaxRate=$totalTaxRate")
        Log.d("PricingCalculation", "=== calculateItemTax END ===")

        return Pair(totalTaxAmount, totalTaxRate)
    }

    /**
     * Calculate complex tax using multiple tax types with edge case handling
     * Applies taxes based on context:
     * - Store taxes: Apply ONLY default taxes (isDefault = true)
     * - Product taxes: Apply ONLY non-default taxes (isDefault = false)
     */
    private fun calculateComplexTax(taxableAmount: Double, taxDetails: List<TaxDetail>, isStoreTax: Boolean = true): Pair<Double, Double> {

        Log.d("PricingCalculation", "=== calculateComplexTax START ===")
        Log.d("PricingCalculation", "TaxableAmount: $taxableAmount")
        Log.d("PricingCalculation", "TaxDetails count: ${taxDetails.size}")
        Log.d("PricingCalculation", "IsStoreTax: $isStoreTax")

        // Edge case: Zero taxable amount or empty tax details
        if (taxableAmount <= 0.0 || taxDetails.isEmpty()) {
            Log.d("PricingCalculation", "Edge case: Zero taxable amount or empty tax details - returning (0.0, 0.0)")
            return Pair(0.0, 0.0)
        }

        var totalTaxAmount = 0.0
        var totalTaxRate = 0.0

        // Apply taxes based on context
        val applicableTaxes = if (isStoreTax) {
            // Store taxes: Apply ONLY default taxes (isDefault = true)
            taxDetails.filter { it.isDefault == true }
        } else {
            // Product taxes: Apply ONLY non-default taxes (isDefault = false)
            taxDetails.filter { it.isDefault == false }
        }

        Log.d("PricingCalculation", "Applicable taxes count: ${applicableTaxes.size}")

        // Edge case: No applicable taxes
        if (applicableTaxes.isEmpty()) {
            Log.d("PricingCalculation", "Edge case: No applicable taxes found - returning (0.0, 0.0)")
            return Pair(0.0, 0.0)
        }

        applicableTaxes.forEach { taxDetail ->
            Log.d("PricingCalculation", "Processing tax: ${taxDetail.title} (${taxDetail.type}, ${taxDetail.value}, isDefault: ${taxDetail.isDefault})")

            when (taxDetail.type?.lowercase()) {
                "percentage" -> {
                    // Edge case: Invalid percentage value
                    val percentageRate = if (taxDetail.value >= 0.0) taxDetail.value / 100.0 else 0.0
                    val taxAmount = taxableAmount * percentageRate
                    totalTaxAmount += taxAmount
                    totalTaxRate += percentageRate
                    Log.d("PricingCalculation", "Percentage tax applied: Rate=$percentageRate, Amount=$taxAmount")
                }
                "fixed amount" -> {
                    // Edge case: Invalid fixed amount
                    val fixedAmount = if (taxDetail.value >= 0.0) taxDetail.value else 0.0
                    totalTaxAmount += fixedAmount
                    Log.d("PricingCalculation", "Fixed amount tax applied: Amount=$fixedAmount")
                }
                else -> {
                    // Default to percentage for unknown types
                    val percentageRate = if (taxDetail.value >= 0.0) taxDetail.value / 100.0 else 0.0
                    val taxAmount = taxableAmount * percentageRate
                    totalTaxAmount += taxAmount
                    totalTaxRate += percentageRate
                    Log.d("PricingCalculation", "Unknown type, defaulting to percentage: Rate=$percentageRate, Amount=$taxAmount")
                }
            }
        }

        // Edge case: Round to avoid floating point precision issues
        val roundedTaxAmount = round(totalTaxAmount * 100.0) / 100.0
        val roundedTaxRate = round(totalTaxRate * 10000.0) / 10000.0

        Log.d("PricingCalculation", "Final result: TotalTaxAmount=$roundedTaxAmount, TotalTaxRate=$roundedTaxRate")
        Log.d("PricingCalculation", "=== calculateComplexTax END ===")

        return Pair(roundedTaxAmount, roundedTaxRate)
    }

    /**
     * Calculate subtotal using appropriate price type (card or cash)
     */
    private fun calculateSubtotal(cartItems: List<CartItem>, useCardPricing: Boolean): Double {
        return cartItems.sumOf { item ->
            // Check for product-level discount using discountType and discountValue
            val hasProductDiscount = item.discountType != null && item.discountValue != null && item.discountValue!! > 0.0
            
            val price = when {
                useCardPricing -> {
                    when {
                        hasProductDiscount -> {
                            // Use getProductDiscountedPrice() for product-level discounts
                            item.getProductDiscountedPrice()
                        }
                        item.isDiscounted && item.discountPrice != null -> {
                            item.discountPrice!!
                        }
                        else -> {
                            item.price ?: 0.0
                        }
                    }
                }
                else -> {
                    when {
                        hasProductDiscount -> {
                            // For cash pricing, calculate discounted price from cashPrice
                            val cashPrice = item.cashPrice
                            when (item.discountType) {
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE -> {
                                    cashPrice - ((cashPrice * (item.discountValue ?: 0.0)) / 100.0)
                                }
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT -> {
                                    cashPrice - (item.discountValue ?: 0.0)
                                }
                                else -> cashPrice
                            }
                        }
                        item.isDiscounted && item.cashDiscountedPrice > 0.0 -> {
                            item.cashDiscountedPrice
                        }
                        else -> {
                            item.cashPrice
                        }
                    }
                }
            }
            price * (item.quantity ?: 1)
        }
    }

    /**
     * Calculate product-level discounts (applied to subtotal but not shown to user)
     */
    private fun calculateProductLevelDiscounts(cartItems: List<CartItem>, useCardPricing: Boolean): Double {
        return cartItems.sumOf { item ->
            // Check for product-level discount using discountType and discountValue
            val hasProductDiscount = item.discountType != null && item.discountValue != null && item.discountValue!! > 0.0
            
            val basePrice = when {
                useCardPricing -> {
                    item.price ?: 0.0
                }
                else -> {
                    item.cashPrice
                }
            }
            
            val itemTotal = basePrice * (item.quantity ?: 1)

            val discount = if (hasProductDiscount) {
                // Calculate discount using discountType and discountValue
                when (item.discountType) {
                    com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE -> {
                        itemTotal * ((item.discountValue ?: 0.0) / 100.0)
                    }
                    com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT -> {
                        (item.discountValue ?: 0.0) * (item.quantity ?: 1)
                    }
                    else -> 0.0
                }
            } else {
                // Fallback to old discount fields for backward compatibility
                when (item.discountType?.name?.trim()?.lowercase()) {
                    "percentage" -> {
                        if (item.fixedPercentageDiscount != null) {
                            itemTotal * (item.fixedPercentageDiscount / 100.0)
                        } else {
                            0.0
                        }
                    }
                    "amount" -> {
                        if (item.fixedDiscount != null) {
                            item.fixedDiscount * (item.quantity ?: 1)
                        } else {
                            0.0
                        }
                    }
                    else -> {
                        item.fixedDiscount ?: 0.0
                    }
                }
            }

            discount
        }
    }

    /**
     * Calculate order-level discount (shown to user)
     */
    private fun calculateOrderLevelDiscount(discountOrder: DiscountOrder?, subtotalAfterProductDiscounts: Double): Double {
        if (discountOrder == null) return 0.0

        return when (discountOrder.discountType?.trim()?.lowercase()) {
            "percentage" -> {
                // Recalculate percentage discount based on current subtotal
                subtotalAfterProductDiscounts * (discountOrder.percentage / 100.0)
            }
            "amount" -> {
                // Fixed amount discount, but cap it at the available subtotal
                minOf(discountOrder.amount, subtotalAfterProductDiscounts)
            }
            else -> {
                // Default to fixed amount, but cap it at the available subtotal
                minOf(discountOrder.amount, subtotalAfterProductDiscounts)
            }
        }
    }

    /**
     * Calculate tax based on taxable items and current subtotal
     */
    private fun calculateTax(
        cartItems: List<CartItem>,
        subtotalAfterDiscounts: Double,
        taxRate: Double,
        useCardPricing: Boolean
    ): Double {
        val taxableItems = cartItems.filter { it.applyTax }

        if (taxableItems.isEmpty()) {
            return 0.0
        }

        // Calculate the ratio of taxable items to total subtotal
        val taxableSubtotal = taxableItems.sumOf { item ->
            val hasProductDiscount = item.discountType != null && item.discountValue != null && item.discountValue!! > 0.0
            
            val price = when {
                useCardPricing -> {
                    when {
                        hasProductDiscount -> {
                            item.getProductDiscountedPrice()
                        }
                        item.isDiscounted && item.discountPrice != null -> {
                            item.discountPrice!!
                        }
                        else -> {
                            item.price ?: 0.0
                        }
                    }
                }
                else -> {
                    when {
                        hasProductDiscount -> {
                            val cashPrice = item.cashPrice
                            when (item.discountType) {
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE -> {
                                    cashPrice - ((cashPrice * (item.discountValue ?: 0.0)) / 100.0)
                                }
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT -> {
                                    cashPrice - (item.discountValue ?: 0.0)
                                }
                                else -> cashPrice
                            }
                        }
                        item.isDiscounted && item.cashDiscountedPrice > 0.0 -> {
                            item.cashDiscountedPrice
                        }
                        else -> {
                            item.cashPrice
                        }
                    }
                }
            }
            price * (item.quantity ?: 1)
        }

        // Apply the same discount ratio to taxable amount
        val originalSubtotal = cartItems.sumOf { item ->
            val hasProductDiscount = item.discountType != null && item.discountValue != null && item.discountValue!! > 0.0
            
            val price = when {
                useCardPricing -> {
                    when {
                        hasProductDiscount -> {
                            item.getProductDiscountedPrice()
                        }
                        item.isDiscounted && item.discountPrice != null -> {
                            item.discountPrice!!
                        }
                        else -> {
                            item.price ?: 0.0
                        }
                    }
                }
                else -> {
                    when {
                        hasProductDiscount -> {
                            val cashPrice = item.cashPrice
                            when (item.discountType) {
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.PERCENTAGE -> {
                                    cashPrice - ((cashPrice * (item.discountValue ?: 0.0)) / 100.0)
                                }
                                com.retail.dolphinpos.domain.model.home.cart.DiscountType.AMOUNT -> {
                                    cashPrice - (item.discountValue ?: 0.0)
                                }
                                else -> cashPrice
                            }
                        }
                        item.isDiscounted && item.cashDiscountedPrice > 0.0 -> {
                            item.cashDiscountedPrice
                        }
                        else -> {
                            item.cashPrice
                        }
                    }
                }
            }
            price * (item.quantity ?: 1)
        }

        val taxableRatio = if (originalSubtotal > 0.0) taxableSubtotal / originalSubtotal else 0.0
        val taxableAmountAfterDiscounts = subtotalAfterDiscounts * taxableRatio

        return taxableAmountAfterDiscounts * taxRate
    }

    /**
     * Switch between card and cash pricing and recalculate
     */
    fun switchPricingType(
        cartItems: List<CartItem>,
        discountOrder: DiscountOrder?,
        rewardAmount: Double,
        useCardPricing: Boolean,
        isTaxApplied: Boolean = true,
        taxRate: Double = 0.10,
        taxDetails: List<TaxDetail>? = null,
        taxExempt: Boolean = false
    ): PricingResult {
        val config = PricingConfiguration(
            isTaxApplied = isTaxApplied,
            taxRate = taxRate,
            useCardPricing = useCardPricing,
            taxDetails = taxDetails,
            taxExempt = taxExempt
        )

        val result = calculatePricing(cartItems, discountOrder, rewardAmount, config)

        // Calculate cash discount (difference between card and cash pricing)
        val cashDiscount = if (!useCardPricing) {
            calculateCashDiscount(cartItems)
        } else {
            0.0
        }

        return result.copy(
            rewardDiscount = result.rewardDiscount + cashDiscount // Include cash discount in reward discount field
        )
    }

    /**
     * Calculate cash discount (difference between card and cash pricing)
     */
    private fun calculateCashDiscount(cartItems: List<CartItem>): Double {
        return cartItems.sumOf { item ->
            val cardPrice = item.price ?: 0.0
            val cashPrice = item.cashPrice ?: cardPrice

            // Calculate the difference per item, then multiply by quantity
            (cardPrice - cashPrice) * (item.quantity ?: 1)
        }
    }

    /**
     * Calculate discounted prices for receipt generation without modifying original CartItem objects
     * This method is specifically for receipt generation to show correct discounted prices
     */
    fun calculateDiscountedPricesForReceipt(
        cartItems: List<CartItem>,
        useCardPricing: Boolean
    ): List<CartItem> {
        return cartItems.map { item ->
            val hasProductLevelDiscount = item.fixedDiscount != null && item.fixedDiscount > 0.0 ||
                    item.fixedPercentageDiscount != null && item.fixedPercentageDiscount > 0.0

            if (hasProductLevelDiscount) {
                val originalPrice = when {
                    useCardPricing -> item.price ?: 0.0
                    else -> item.cashPrice
                }

                val discountedPrice = when (item.discountType?.name?.trim()?.lowercase()) {
                    "percentage" -> {
                        if (item.fixedPercentageDiscount != null) {
                            originalPrice * (1 - item.fixedPercentageDiscount / 100.0)
                        } else {
                            originalPrice
                        }
                    }
                    "amount" -> {
                        if (item.fixedDiscount != null) {
                            originalPrice - item.fixedDiscount
                        } else {
                            originalPrice
                        }
                    }
                    else -> {
                        if (item.fixedDiscount != null) {
                            originalPrice - item.fixedDiscount
                        } else {
                            originalPrice
                        }
                    }
                }

                // Create a copy with updated discountPrice for receipt generation
                if (useCardPricing) {
                    item.copy(
                        discountPrice = discountedPrice,
                        isDiscounted = true
                    )
                } else {
                    item.copy(
                        cashDiscountedPrice = discountedPrice,
                        isDiscounted = true
                    )
                }
            } else {
                // Return original item if no product-level discount
                item
            }
        }
    }
}