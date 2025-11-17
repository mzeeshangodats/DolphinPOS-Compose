package com.retail.dolphinpos.data.setup.hardware.receipt

import android.annotation.SuppressLint
import android.util.Log
import com.retail.dolphinpos.common.utils.applyStrikethrough
import com.retail.dolphinpos.domain.model.order.PendingOrder
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class GenerateReceiptTextUseCase @Inject constructor(
    private val getStoreDetailsFromLocalUseCase: GetStoreDetailsFromLocalUseCase,
    //private val pricingCalculationUseCase: PricingCalculationUseCase,
    //private val generateSplitPaymentReceiptTextUseCase: GenerateSplitPaymentReceiptTextUseCase
) {

    companion object {
        private const val TAG = "GenerateReceiptTextUseCase"
    }
//    private var isCashPayment: Boolean = false

    @SuppressLint("DefaultLocale", "SimpleDateFormat")
    suspend operator fun invoke(
        order: PendingOrder,
        isReceiptForRefund: Boolean = false
    ): String {
        Log.d(TAG, "invoke: Generating receipt for PendingOrder: ${order.orderNumber}")
        return generatePendingOrderReceipt(order, isReceiptForRefund)
    }

    /*@SuppressLint("DefaultLocale", "SimpleDateFormat")
    operator fun invoke(
        order: SingleOrder,
        isReceiptForRefund: Boolean = false
    ): String {
        // Check if this is a split payment order
        if (order.isSplit()) {
            return generateSplitPaymentReceiptTextUseCase(order, isReceiptForRefund)
        }

        val store = order.store
        val orderItems = order.orderItems ?: emptyList()
        val storePolicy = getStoreDetailsFromLocalUseCase()?.policy ?: ""

        val isCashPayment = order.paymentMethod.equals("CASH", ignoreCase = true)

        val date =
            if (isReceiptForRefund) getCurrentDateUsFormat() else order.createdAt?.toOrderDateFormat()
                ?: getCurrentDateUsFormat()
        val time =
            if (isReceiptForRefund) getCurrentTimeFormatted() else order.createdAt?.formatTimeString()
                ?: getCurrentTimeFormatted()

        // Convert order items to cart items for receipt pricing calculation
        val cartItems = orderItems.map { orderItem ->
            CartItem(
                productId = orderItem.product?.id,
                productVariantId = orderItem.productVariant?.id,
                productVariantName = orderItem.productVariant?.title,
                name = orderItem.product?.name,
                price = orderItem.price?.toDoubleOrNull(),
                quantity = orderItem.quantity,
                imageUrl = orderItem.product?.images?.firstOrNull()?.fileURL,
                cashPrice = orderItem.cashPrice,
                cashDiscountedPrice = orderItem.cashDiscountedPrice,
                discountPrice = orderItem.discountedPrice,
                isDiscounted = orderItem.isDiscounted,
                fixedDiscount = orderItem.fixedDiscount,
                fixedPercentageDiscount = orderItem.fixedPercentageDiscount,
                discountType = orderItem.discountType,
                discountReason = orderItem.discountReason,
                applyTax = true,
                isCustom = false,
                reason = null,
                barCode = null,
                discountId = null,
                images = null,
                costPrice = 0.0,
                sku = "",
                removeFromCart = false,
                // Product-level tax fields
                productTaxAmount = orderItem.productTaxAmount,
                productTaxRate = orderItem.productTaxRate,
                productTaxableAmount = orderItem.productTaxableAmount,
                productTaxDetails = orderItem.productTaxDetails
            )
        }

        // Get correctly calculated discounted prices for receipt
        val receiptCartItems = pricingCalculationUseCase.calculateDiscountedPricesForReceipt(
            cartItems = cartItems,
            useCardPricing = !isCashPayment
        )

        return buildString {
            val receiptWidth = 48
            val divider = "-".repeat((receiptWidth - 4)) + "\n"

            append("\n")

            append(centerAlign(store?.name ?: getStoreDetailsFromLocalUseCase()?.name ?: "") + "\n")
            append(
                centerAlign(
                    store?.address ?: getStoreDetailsFromLocalUseCase()?.address ?: ""
                ) + "\n\n"
            )

            append("Order No   : ${order.orderNumber?.take(30) ?: ""}\n")
            append(
                String.format(
                    Locale.US,
                    "%-20s %15s\n",
                    "Date : $date",
                    "Time : $time"
                )
            )

            append("\n")
            append(formatBoxedTotalForPrint(order.total ?: 0.0, isReceiptForRefund))

            val receiptTitle = if (isReceiptForRefund) "REFUND RECEIPT" else "RECEIPT"
            append("\n\n${centerAlign(receiptTitle)}\n\n")

            append(
                String.format(
                    Locale.US,
                    "%-26s %s\n",
                    "Items", "Price"
                )
            )
            append(createStraightLine())

            // Use the receipt cart items with correct discounted prices
            receiptCartItems
                .groupBy {
                    val product = it.productId
                    val variant = it.productVariantId
                    if (isReceiptForRefund) {
                        if (variant != null) "variant:$variant" else "product:$product"
                    } else
                        if (variant != null) "variant:$variant" else "product:$product"

                }
                .map { (_, items) ->
                    items.first().copy(
                        quantity = items.sumOf { it.quantity ?: 0 }
                    )
                }
                .forEach { item ->
                    val price = item.price ?: 0.0
                    val quantity = item.quantity?.toDouble() ?: 0.0
                    val isDiscounted = item.isDiscounted
                    val discountedPrice = item.discountPrice

                    // Check for product-level discounts (fixed discount or percentage discount)
                    val hasProductLevelDiscount =
                        item.fixedDiscount != null && item.fixedDiscount > 0.0 ||
                                item.fixedPercentageDiscount != null && item.fixedPercentageDiscount > 0.0

                    val effectivePrice = price
                    val effectiveTotal = price * quantity

                    // Show product-level discounts with strikethrough
                    if (hasProductLevelDiscount) {
                        val productNameString = item.name ?: "-"
                        val parts = productNameString.split("\n", limit = 2)
                        val mainProductName = parts[0].trim()

                        // Show original price with strikethrough
                        append(
                            formatReceiptEntry(
                                mainProductName,
                                effectiveTotal,
                                true,
                                isReceiptForRefund
                            )
                        )
                        if (parts.size > 1) {
                            append(
                                String.format(
                                    Locale.US,
                                    "%-26s\n", // Indent variant slightly
                                    parts[1].trim()
                                )
                            )
                        } else {
                            append(
                                String.format(
                                    Locale.US,
                                    "%-26s\n",
                                    item.productVariantName?.let { "Variant: $it" } ?: "-",
                                )
                            )
                        }
                        // (quantity x price) WITHOUT strikethrough
                        if (quantity > 1) {

                        append(
                            String.format(
                                Locale.US,
                                "%-26s\n",
                                "(${quantity} x ${String.format(Locale.US, "$%.2f", effectivePrice)})"
                            )
                        )

                            *//*if (isReceiptForRefund) {
                                append(
                                    String.format(
                                        Locale.US,
                                    "(${quantity} x ${String.format(Locale.US, "-$%.2f", effectivePrice)})"
                                    )
                                )
                            } else {
                        append(
                            String.format(
                                Locale.US,
                                "%-26s\n",
                                "(${quantity} x ${String.format(Locale.US, "$%.2f", effectivePrice)})"
                            )
                        )
                            }*//*
                        }

                        // Show discount line with discounted price
                        val discountType = when {
                            item.fixedPercentageDiscount != null && item.fixedPercentageDiscount > 0.0 ->
                                "Discount ${item.fixedPercentageDiscount}% off"

                            item.fixedDiscount != null && item.fixedDiscount > 0.0 ->
                                "Discount $${item.fixedDiscount * quantity} off"

                            else -> "Discount applied"
                        }

                        // Calculate the actual discounted total (e.g., $89.95 * 0.95 = $85.45)
                        val actualDiscountedTotal: Double = if (!isCashPayment) {
                            (item.discountPrice ?: price) * quantity
                        } else {
                            // Calculate the discounted price correctly for cash payments
                            val originalTotal = price * quantity
                            val discountAmount = when {
                                item.fixedPercentageDiscount != null && item.fixedPercentageDiscount > 0.0 -> {
                                    originalTotal * (item.fixedPercentageDiscount / 100.0)
                                }

                                item.fixedDiscount != null && item.fixedDiscount > 0.0 -> {
                                    item.fixedDiscount * quantity
                                }

                                else -> 0.0
                            }
                            originalTotal - discountAmount
                        }

                        append(
                            formatReceiptEntry(
                                discountType,
                                actualDiscountedTotal,
                                isReceiptForRefund = isReceiptForRefund,
                            )
                        )
                        append(divider)
                    } else if (isDiscounted && discountedPrice != null) {

                        val effectiveDiscountedTotal =
                            if (isReceiptForRefund) discountedPrice * quantity else discountedPrice * quantity

                        val formattedEffectivePrice =
                            formatCurrency(effectivePrice).applyStrikethrough()
                        val productNameString = item.name ?: "-"
                        val parts =
                            productNameString.split("\n", limit = 2)
                        val mainProductName = parts[0].trim()

                        append(
                            formatReceiptEntry(
                                mainProductName,
                                effectiveDiscountedTotal,
                                isReceiptForRefund = isReceiptForRefund,
                            )
                        )
                        if (parts.size > 1)
                            append(
                                String.format(
                                    Locale.US,
                                    "%-26s\n", // Indent variant slightly
                                    parts[1].trim()
                                )
                            )
                        else {
                            append(
                                String.format(
                                    Locale.US,
                                    "%-26s\n",
                                    item.productVariantName?.let { "Variant: $it" } ?: "-",
                                )
                            )
                        }
                        // (quantity x price)
                        if (quantity > 1) {
                            append(
                                String.format(
                                    Locale.US,
                                    "%-26s\n",
                                    "(${quantity} x $formattedEffectivePrice)"
                                )
                            )

                            *//*if (isReceiptForRefund) {
                                append(
                                    String.format(
                                        Locale.US,
                                        "(${quantity} x $formattedEffectivePrice)"
                                    )
                                )
                            } else {
                                append(
                                    String.format(
                                        Locale.US,
                                        "(${quantity} x $formattedEffectivePrice)"
                                    )
                                )
                            }*//*
                        }
                        append(divider)
                    } else {
                        // Regular item without discounts
                        val productNameString = item.name ?: "-"
                        val parts =
                            productNameString.split("\n", limit = 2)
                        val mainProductName = parts[0].trim()

                        append(
                            formatReceiptEntry(
                                mainProductName,
                                effectiveTotal,
                                isReceiptForRefund = isReceiptForRefund
                            )
                        )
                        if (parts.size > 1)
                            append(
                                String.format(
                                    Locale.US,
                                    "%-26s\n", // Indent variant slightly
                                    parts[1].trim()
                                )
                            )
                        else {
                            append(
                                String.format(
                                    Locale.US,
                                    "%-26s\n",
                                    item.productVariantName?.let { "Variant: $it" } ?: "-",
                                )
                            )
                        }
                        // (quantity x price) without strikethrough
                        if (quantity > 1) {
                            append(
                                String.format(
                                    Locale.US,
                                    "%-26s\n",
                                    "(${quantity} x ${formatCurrency(effectivePrice)})"
                                )
                            )
                        }
                        append(divider)
                    }
                }

            val subtotal = order.subTotal ?: 0.0
            val tax = order.taxValue ?: 0.0
            val discount = order.discountAmount ?: 0.0
            val total = order.total ?: 0.0

            val totalAmountLabel =
                if (isReceiptForRefund) "TOTAL REFUND AMOUNT:" else "TOTAL AMOUNT:"

            if (discount != 0.0) {
                append("\n")
            val discountValue = if (isReceiptForRefund) {
                String.format(Locale.US, "-$%.2f", discount)
            } else {
                String.format(Locale.US, "-$%.2f", discount)
            }
            append("${"DISCOUNT:".padEnd(25)} $discountValue\n")
            }
            append("\n")
            append(
            run {
                val subtotalValue = if (isReceiptForRefund) {
                    String.format(Locale.US, "-$%.2f", subtotal)
                } else {
                    String.format(Locale.US, "$%.2f", subtotal)
                }
                "${"SUBTOTAL:".padEnd(25)} $subtotalValue\n"
            }
            )
            
            // Enhanced tax breakdown display - aggregate and show ALL taxes once
            if (tax > 0.0) {
                append("\n")
                append("TAX BREAKDOWN:\n")
                
                // Aggregate all taxes (both store and product level) by description
                val taxMap = mutableMapOf<String, Double>()
                
                // Add store-level taxes from order.taxDetails (default only)
                order.taxDetails?.filter { it.isDefault == true }?.forEach { taxDetail ->
                    val taxAmount = taxDetail.amount ?: when (taxDetail.type?.lowercase()) {
                        "percentage" -> {
                            val rate = taxDetail.value / 100.0
                            subtotal * rate
                        }
                        "fixed amount" -> taxDetail.value
                        else -> {
                            val rate = taxDetail.value / 100.0
                            subtotal * rate
                        }
                    }
                    
                    val taxValue = taxDetail.value
                    val taxType = taxDetail.type ?: "Percentage"
                    val taxDescription = when (taxType.lowercase()) {
                        "percentage" -> "${taxDetail.title} ($taxValue%)"
                        "fixed amount" -> "${taxDetail.title} ($$taxValue)"
                        else -> "${taxDetail.title} ($taxValue%)"
                    }
                    taxMap[taxDescription] = (taxMap[taxDescription] ?: 0.0) + taxAmount
                }
                
                // Add product-level taxes from orderItems.appliedTaxes
                orderItems.forEach { orderItem ->
                    orderItem.appliedTaxes?.forEach { productTaxDetail ->
                        val productTaxAmount = productTaxDetail.amount ?: when (productTaxDetail.type?.lowercase()) {
                            "percentage" -> {
                                val rate = productTaxDetail.value / 100.0
                                (orderItem.price?.toDoubleOrNull() ?: 0.0) * rate
                            }
                            "fixed amount" -> productTaxDetail.value
                            else -> {
                                val rate = productTaxDetail.value / 100.0
                                (orderItem.price?.toDoubleOrNull() ?: 0.0) * rate
                            }
                        }
                        
                        val productTaxValue = productTaxDetail.value
                        val productTaxType = productTaxDetail.type ?: "Percentage"
                        val productTaxDescription = when (productTaxType.lowercase()) {
                            "percentage" -> "${productTaxDetail.title} ($productTaxValue%)"
                            "fixed amount" -> "${productTaxDetail.title} ($$productTaxValue)"
                            else -> "${productTaxDetail.title} ($productTaxValue%)"
                        }
                        taxMap[productTaxDescription] = (taxMap[productTaxDescription] ?: 0.0) + productTaxAmount
                    }
                }
                
                // Display all taxes together
                if (taxMap.isNotEmpty()) {
                    taxMap.forEach { (description, amount) ->
                        val formattedAmount = if (isReceiptForRefund) {
                            String.format(Locale.US, "-$%.2f", amount)
                        } else {
                            String.format(Locale.US, "$%.2f", amount)
                        }
                        append(formatTaxLineWithWrapping("$description:", formattedAmount))
                    }
                } else {
                    // Fallback to simple tax display
                    val storeData = getStoreDetailsFromLocalUseCase()
                    val taxRate = storeData?.taxValue ?: 0.0
                    val taxDescription = if (taxRate > 0.0) "Tax ($taxRate%)" else "Tax"
                    
                    val taxValue = if (isReceiptForRefund) {
                        String.format(Locale.US, "-$%.2f", tax)
                    } else {
                        String.format(Locale.US, "$%.2f", tax)
                    }
                    append("${"  $taxDescription:".padEnd(25)} $taxValue\n")
                }
            } else {
                // No tax case
                append("\n")
                val taxValue = if (isReceiptForRefund) {
                    String.format(Locale.US, "-$%.2f", tax)
                } else {
                    String.format(Locale.US, "$%.2f", tax)
                }
                append("${"TAX:".padEnd(25)} $taxValue\n")
            }

            // Combine reward discount and order-level discount into a single DISCOUNT line

            append("\n")
            append(divider)
            val totalFormatted = if (isReceiptForRefund) {
                String.format(Locale.US, "-$%.2f", total)
            } else {
                String.format(Locale.US, "$%.2f", total)
            }
            append("${totalAmountLabel.padEnd(25)} $totalFormatted\n")
            append(divider)
            val paymentTotal = if (isReceiptForRefund) {
                String.format(Locale.US, "-$%.2f", total)
            } else {
                String.format(Locale.US, "$%.2f", total)
            }
            val paymentLabel = order.paymentMethod?.uppercase(Locale.getDefault()) ?: "-"
            append("${paymentLabel.padEnd(25)} $paymentTotal\n")
            append("\n")

            order.customer?.let { customer ->

                customer.phoneNumber?.let {

                    val redeemedPoints = order.redeemPoints ?: 0
                    val balancePoints = customer.pointsEarned?.toInt() ?: 0

                    append(divider)
                    append("REWARD MEMBER DETAILS:")
                    append("\n\n")
                    if (redeemedPoints > 0) {
                        append(
                            String.format(
                                Locale.US,
                                "%-25s %s\n",
                                "Redeemed Points :",
                                "$redeemedPoints PTS"
                            )
                        )
                    }
                    append(String.format(Locale.US, "%-25s %s\n", "Balance :", "$balancePoints PTS"))
                    append(divider)
                    append("\n")

                }

            }

            storePolicy.split(" ")
                .chunked(5)
                .joinToString("\n") { line ->
                    centerAlign(line.joinToString(" "))
                }
                .let { append(it) }
                .append("\n")

        }
    }*/

    @SuppressLint("DefaultLocale")
    private fun formatReceiptEntry(
        mainProductName: String,
        effectiveTotal: Double,
        isStrikethrough: Boolean = false,
        isReceiptForRefund: Boolean,
    ): String {
        val productNameColWidth = 30
        val totalColWidth = 10

        val formattedOutput = StringBuilder()
        val formattedTotal = if (isStrikethrough) {
            formatCurrency(effectiveTotal).applyStrikethrough()
        } else {
            if (isReceiptForRefund) String.format(
                Locale.US,
                "-$%.2f",
                effectiveTotal
            ) else formatCurrency(effectiveTotal)
        }

        if (mainProductName.length <= productNameColWidth) {
            formattedOutput.append(
                String.format(
                    Locale.US,
                    "%-${productNameColWidth}s %${totalColWidth}s\n",
                    mainProductName,
                    formattedTotal
                )
            )
        } else {
            var firstLineName: String
            var remainingName: String

            var splitPoint = mainProductName.lastIndexOf(" ", productNameColWidth)
            if (splitPoint == -1 || splitPoint < (productNameColWidth / 2)) {
                splitPoint = productNameColWidth
            }

            firstLineName = mainProductName.substring(0, splitPoint).trim()
            remainingName = mainProductName.substring(splitPoint).trim()

            formattedOutput.append(
                String.format(
                    Locale.US,
                    "%-${productNameColWidth}s %${totalColWidth}s\n",
                    firstLineName,
                    formattedTotal
                )
            )
            formattedOutput.append(
                String.format(Locale.US, "%-${productNameColWidth}s\n", remainingName)
            )
        }

        return formattedOutput.toString()
    }

    private fun centerAlign(text: String): String {
        val receiptWidth = 40
        if (text.length >= receiptWidth) {
            return text
        }

        val totalPadding = receiptWidth - text.length
        val leftPadding = totalPadding / 2
        val rightPadding = totalPadding - leftPadding

        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding)
    }

    @SuppressLint("DefaultLocale")
    private fun formatBoxedTotalForPrint(total: Double, isReceiptForRefund: Boolean): String {
        val totalWidth = 40
        val innerWidth = totalWidth - 2

        val totalLabel = "TOTAL"
        val formattedAmount = String.format(Locale.US, "$%.2f", total)
        val displayAmount = if (isReceiptForRefund) "-$formattedAmount" else formattedAmount

        val topBorder = "+" + "-".repeat(innerWidth) + "+"
        val bottomBorder = "+" + "-".repeat(innerWidth) + "+"

        val totalLabelPadding = (innerWidth - totalLabel.length) / 2
        val totalLabelLine = "|" + " ".repeat(totalLabelPadding) + totalLabel +
                " ".repeat(innerWidth - totalLabel.length - totalLabelPadding) + "|"

        val amountPadding = (innerWidth - displayAmount.length) / 2
        val amountLine =
            "|" + " ".repeat(amountPadding) +
                    displayAmount +
                    " ".repeat(
                        innerWidth - displayAmount.length - amountPadding
                    ) + "|"

        return "$topBorder\n$totalLabelLine\n$amountLine\n$bottomBorder"
    }

    private fun createStraightLine(): String {
        val totalWidth = 40
        return "─".repeat(totalWidth - 4) + "\n"
    }

    /**
     * Formats a tax line with proper alignment:
     * - Tax name starts on the left
     * - Tax value aligned to the right
     * - If tax name is too long, it wraps to next line but value stays on the right
     *
     * Receipt width: 40 characters
     * Format: "Tax Name:                  $10.00"
     */
    private fun formatTaxLineWithWrapping(taxName: String, taxValue: String): String {
        val receiptWidth = 40
        val valueWidth = taxValue.length
        val maxNameWidth = receiptWidth - valueWidth - 1 // -1 for space between name and value

        return if (taxName.length <= maxNameWidth) {
            val padding = receiptWidth - taxName.length - valueWidth
            String.format(Locale.US, "%s%s%s\n", taxName, " ".repeat(padding), taxValue)
        } else {
            val wrappedName = wrapText(taxName, receiptWidth - valueWidth - 1)
            val lines = wrappedName.split("\n")

            if (lines.size == 1) {
                val padding = receiptWidth - lines[0].length - valueWidth
                String.format(Locale.US, "%s%s%s\n", lines[0], " ".repeat(padding), taxValue)
            } else {
                val result = StringBuilder()
                for (i in 0 until lines.size - 1) {
                    result.append(lines[i]).append("\n")
                }
                val lastLine = lines.last()
                val padding = receiptWidth - lastLine.length - valueWidth
                result.append(
                    String.format(
                        Locale.US,
                        "%s%s%s\n",
                        lastLine,
                        " ".repeat(padding),
                        taxValue
                    )
                )
                result.toString()
            }
        }
    }

    /**
     * Wraps text to fit within specified width
     */
    private fun wrapText(text: String, maxWidth: Int): String {
        if (text.length <= maxWidth) return text

        val result = StringBuilder()
        var currentLine = ""
        val words = text.split(" ")

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine = word
            } else if ((currentLine.length + 1 + word.length) <= maxWidth) {
                currentLine += " $word"
            } else {
                result.append(currentLine).append("\n")
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            result.append(currentLine)
        }

        return result.toString()
    }

    private fun formatCurrency(value: Double): String =
        "$" + String.format(Locale.US, "%.2f", value)

    private fun formatNegativeCurrency(value: Double): String =
        "-" + formatCurrency(value)

    private fun formatLine(label: String, value: String, labelWidth: Int = 26): String {
        val paddedLabel = if (label.length >= labelWidth) label else label.padEnd(labelWidth, ' ')
        return "$paddedLabel $value\n"
    }

    @SuppressLint("DefaultLocale", "SimpleDateFormat")
    private suspend fun generatePendingOrderReceipt(
        order: PendingOrder,
        isReceiptForRefund: Boolean = false
    ): String {
        Log.d(
            TAG,
            "generatePendingOrderReceipt: Starting receipt generation for order: ${order.orderNumber}"
        )

        return try {
            val store = getStoreDetailsFromLocalUseCase()
            val storeName = store?.name ?: ""
            val storeLocation = store?.address ?: ""
            val storePolicy = store?.policy ?: ""

            Log.d(
                TAG,
                "generatePendingOrderReceipt: Store details - Name: $storeName, Location: $storeLocation"
            )

            val isCashPayment = order.paymentMethod.equals("CASH", ignoreCase = true)
            Log.d(
                TAG,
                "generatePendingOrderReceipt: Payment method: ${order.paymentMethod}, IsCash: $isCashPayment"
            )

            // Format date and time from timestamp
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = dateFormat.format(java.util.Date(order.createdAt))
            val time = timeFormat.format(java.util.Date(order.createdAt))
            Log.d(TAG, "generatePendingOrderReceipt: Order date: $date, time: $time")

            val receiptWidth = 48
            val divider = "-".repeat((receiptWidth - 4)) + "\n"

            buildString {
                append("\n")

                // Store name and location
                append(centerAlign(storeName) + "\n")
                append(centerAlign(storeLocation) + "\n\n")

                // Order number, date, and time
                append("Order No   : ${order.orderNumber.take(30)}\n")
                append(
                    String.format(
                        Locale.US,
                        "%-20s %15s\n",
                        "Date : $date",
                        "Time : $time"
                    )
                )

                append("\n")
                append(formatBoxedTotalForPrint(order.total, isReceiptForRefund))

                val receiptTitle = if (isReceiptForRefund) "REFUND RECEIPT" else "RECEIPT"
                append("\n\n${centerAlign(receiptTitle)}\n\n")

                // Items header
                append(
                    String.format(
                        Locale.US,
                        "%-26s %s\n",
                        "Items", "Price"
                    )
                )
                append(createStraightLine())

                // Order items
                if (order.items.isEmpty()) {
                    Log.w(TAG, "generatePendingOrderReceipt: Order has no items")
                    append("No items in order\n")
                } else {
                    Log.d(TAG, "generatePendingOrderReceipt: Processing ${order.items.size} items")
                    order.items.forEachIndexed { index, item ->
                        val itemName = item.name ?: "Unknown Item"
                        val quantity = item.quantity ?: 0
                        val price = item.price ?: 0.0
                        val discountedPrice = item.discountedPrice
                        val itemTotal = if (discountedPrice != null && discountedPrice > 0.0) {
                            discountedPrice * quantity
                        } else {
                            price * quantity
                        }

                        Log.d(
                            TAG,
                            "generatePendingOrderReceipt: Item $index - Name: $itemName, Qty: $quantity, Price: $price, Total: $itemTotal"
                        )

                        // Format item entry
                        val itemNameFormatted = if (itemName.length > 26) {
                            itemName.take(23) + "..."
                        } else {
                            itemName
                        }

                        append(formatLine(itemNameFormatted, formatCurrency(itemTotal)))

                        // Show quantity if more than 1
                        if (quantity > 1) {
                            val unitPrice = if (discountedPrice != null && discountedPrice > 0.0) {
                                discountedPrice
                            } else {
                                price
                            }
                            append("  ($quantity x ${formatCurrency(unitPrice)})\n")
                        }

                        // Show discount if applicable
                        if (discountedPrice != null && discountedPrice > 0.0 && discountedPrice < price) {
                            val discountAmount = (price - discountedPrice) * quantity
                            append(formatLine("  Discount", formatNegativeCurrency(discountAmount)))
                        }

                        append(divider)
                    }
                }

                // Totals section
                append("\n")
                append(createStraightLine())

                // Subtotal
                append(formatLine("Subtotal", formatCurrency(order.subTotal)))

                // Cash discount
                if (isCashPayment && order.cashDiscountAmount > 0.0) {
                    Log.d(
                        TAG,
                        "generatePendingOrderReceipt: Cash discount: ${order.cashDiscountAmount}"
                    )
                    append(
                        formatLine(
                            "Cash Discount",
                            formatNegativeCurrency(order.cashDiscountAmount)
                        )
                    )
                }

                // Order discount
                if (order.discountAmount > 0.0) {
                    Log.d(
                        TAG,
                        "generatePendingOrderReceipt: Order discount: ${order.discountAmount}"
                    )
                    append(formatLine("Discount", formatNegativeCurrency(order.discountAmount)))
                }

                // Tax breakdown or Tax Exempt
                val isTaxExempt = !order.applyTax || order.taxValue == 0.0
                
                if (isTaxExempt) {
                    // Tax Exempt case
                    Log.d(TAG, "generatePendingOrderReceipt: Tax Exempt")
                    append(formatLine("Tax", "EXEMPT"))
                } else if (order.taxValue > 0.0) {
                    Log.d(TAG, "generatePendingOrderReceipt: Tax: ${order.taxValue}")

                    // Aggregate all taxes from orderItems.appliedTaxes (which includes both store and product taxes)
                    // This ensures we use the actual calculated tax amounts per item, avoiding double-counting
                    val taxMap = mutableMapOf<String, Double>()

                    order.items.forEach { orderItem ->
                        orderItem.appliedTaxes?.forEach { taxDetail ->
                            // Use the amount from taxDetail if available (pre-calculated per item)
                            // Otherwise calculate based on item price and quantity
                            val taxAmount = taxDetail.amount ?: when (taxDetail.type?.lowercase()) {
                                "percentage" -> {
                                    val rate = taxDetail.value / 100.0
                                    val itemPrice =
                                        orderItem.discountedPrice ?: orderItem.price ?: 0.0
                                    itemPrice * (orderItem.quantity ?: 1) * rate
                                }

                                "fixed amount" -> taxDetail.value * (orderItem.quantity ?: 1)
                                else -> {
                                    val rate = taxDetail.value / 100.0
                                    val itemPrice =
                                        orderItem.discountedPrice ?: orderItem.price ?: 0.0
                                    itemPrice * (orderItem.quantity ?: 1) * rate
                                }
                            }

                            val taxValue = taxDetail.value
                            val taxType = taxDetail.type ?: "Percentage"
                            val taxDescription = when (taxType.lowercase()) {
                                "percentage" -> "${taxDetail.title} ($taxValue%)"
                                "fixed amount" -> "${taxDetail.title} ($$taxValue)"
                                else -> "${taxDetail.title} ($taxValue%)"
                            }
                            // Aggregate taxes by description (same tax from multiple items will be summed)
                            taxMap[taxDescription] = (taxMap[taxDescription] ?: 0.0) + taxAmount
                        }
                    }

                    // If no item-level taxes, try using order-level taxDetails as fallback
                    if (taxMap.isEmpty() && order.taxDetails != null
                        && (order.taxDetails?.isNotEmpty() ?: false)
                    ) {
                        order.taxDetails.let {
                            it?.forEach { taxDetail ->
                                val taxAmount =
                                    taxDetail.amount ?: when (taxDetail.type?.lowercase()) {
                                        "percentage" -> {
                                            val rate = taxDetail.value / 100.0
                                            order.subTotal * rate
                                        }

                                        "fixed amount" -> taxDetail.value
                                        else -> {
                                            val rate = taxDetail.value / 100.0
                                            order.subTotal * rate
                                        }
                                    }

                                val taxValue = taxDetail.value
                                val taxType = taxDetail.type ?: "Percentage"
                                val taxDescription = when (taxType.lowercase()) {
                                    "percentage" -> "${taxDetail.title} ($taxValue%)"
                                    "fixed amount" -> "${taxDetail.title} ($$taxValue)"
                                    else -> "${taxDetail.title} ($taxValue%)"
                                }
                                taxMap[taxDescription] = (taxMap[taxDescription] ?: 0.0) + taxAmount
                            }
                        }
                    }

                    // Display tax breakdown if available, otherwise show simple tax
                    if (taxMap.isNotEmpty()) {
                        append("\n")
                        append("TAX BREAKDOWN:\n")
                        taxMap.forEach { (description, amount) ->
                            val formattedAmount = if (isReceiptForRefund) {
                                formatNegativeCurrency(amount)
                            } else {
                                formatCurrency(amount)
                            }
                            // Use formatLine for consistent alignment
                            val descriptionFormatted = if (description.length > 26) {
                                description.take(23) + "..."
                            } else {
                                description
                            }
                            append(formatLine(descriptionFormatted, formattedAmount))
                        }
                    } else {
                        // Fallback to simple tax display
                        append(formatLine("Tax", formatCurrency(order.taxValue)))
                    }
                } else {
                    // Tax is 0 but applyTax is true (shouldn't happen normally, but handle it)
                    Log.d(TAG, "generatePendingOrderReceipt: Tax is 0")
                    append(formatLine("Tax", "$0.00"))
                }

                append(createStraightLine())

                // Total
                append(formatLine("TOTAL", formatCurrency(order.total)))

                append(createStraightLine())
                append("\n")

                // Payment method
                append("Payment Method: ${order.paymentMethod.uppercase()}\n")

                // Card details if available
                order.cardDetails?.let { cardDetails ->
                    Log.d(TAG, "generatePendingOrderReceipt: Card details available")
                    cardDetails.authCode?.let {
                        append("Auth Code: $it\n")
                    }
                    cardDetails.last4?.let {
                        append("Card: ****$it\n")
                    }
                    cardDetails.brand?.let {
                        append("Brand: $it\n")
                    }
                }

                // Transaction ID
                order.transactionId?.let {
                    append("Transaction ID: $it\n")
                }

                append("\n")

                // Store policy
                if (storePolicy.isNotEmpty()) {
                    append(centerAlign(storePolicy) + "\n\n")
                }

                append("Thank you for your business!\n")
                append("\n\n")
            }.also {
                Log.d(
                    TAG,
                    "generatePendingOrderReceipt: Receipt text generated successfully, length: ${it.length}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "generatePendingOrderReceipt: Error generating receipt text", e)
            "Error generating receipt: ${e.message}\n"
        }
    }

}