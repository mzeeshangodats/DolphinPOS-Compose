//package com.lingeriepos.common.usecases.printer.receipt
//
//import android.annotation.SuppressLint
//import com.lingeriepos.common.usecases.PricingCalculationUseCase
//import com.lingeriepos.common.usecases.store.GetStoreDetailsFromLocalUseCase
//import com.lingeriepos.models.model.CartItem
//import com.lingeriepos.models.response.SingleOrder
//import com.lingeriepos.utils.applyStrikethrough
//import com.lingeriepos.utils.formatTimeString
//import com.lingeriepos.utils.getCurrentDateUsFormat
//import com.lingeriepos.utils.getCurrentTimeFormatted
//import com.lingeriepos.utils.toOrderDateFormat
//import java.util.Locale
//import javax.inject.Inject
//
//class GenerateReceiptTextUseCase @Inject constructor(
//    private val getStoreDetailsFromLocalUseCase: GetStoreDetailsFromLocalUseCase,
//    private val pricingCalculationUseCase: PricingCalculationUseCase,
//    private val generateSplitPaymentReceiptTextUseCase: GenerateSplitPaymentReceiptTextUseCase
//) {
////    private var isCashPayment: Boolean = false
//
//    @SuppressLint("DefaultLocale", "SimpleDateFormat")
//    operator fun invoke(
//        order: SingleOrder,
//        isReceiptForRefund: Boolean = false
//    ): String {
//        // Check if this is a split payment order
//        if (order.isSplit()) {
//            return generateSplitPaymentReceiptTextUseCase(order, isReceiptForRefund)
//        }
//
//        val store = order.store
//        val orderItems = order.orderItems ?: emptyList()
//        val storePolicy = getStoreDetailsFromLocalUseCase()?.policy ?: ""
//
//        val isCashPayment = order.paymentMethod.equals("CASH", ignoreCase = true)
//
//        val date =
//            if (isReceiptForRefund) getCurrentDateUsFormat() else order.createdAt?.toOrderDateFormat()
//                ?: getCurrentDateUsFormat()
//        val time =
//            if (isReceiptForRefund) getCurrentTimeFormatted() else order.createdAt?.formatTimeString()
//                ?: getCurrentTimeFormatted()
//
//        // Convert order items to cart items for receipt pricing calculation
//        val cartItems = orderItems.map { orderItem ->
//            CartItem(
//                productId = orderItem.product?.id,
//                productVariantId = orderItem.productVariant?.id,
//                productVariantName = orderItem.productVariant?.title,
//                name = orderItem.product?.name,
//                price = orderItem.price?.toDoubleOrNull(),
//                quantity = orderItem.quantity,
//                imageUrl = orderItem.product?.images?.firstOrNull()?.fileURL,
//                cashPrice = orderItem.cashPrice,
//                cashDiscountedPrice = orderItem.cashDiscountedPrice,
//                discountPrice = orderItem.discountedPrice,
//                isDiscounted = orderItem.isDiscounted,
//                fixedDiscount = orderItem.fixedDiscount,
//                fixedPercentageDiscount = orderItem.fixedPercentageDiscount,
//                discountType = orderItem.discountType,
//                discountReason = orderItem.discountReason,
//                applyTax = true,
//                isCustom = false,
//                reason = null,
//                barCode = null,
//                discountId = null,
//                images = null,
//                costPrice = 0.0,
//                sku = "",
//                removeFromCart = false,
//                // Product-level tax fields
//                productTaxAmount = orderItem.productTaxAmount,
//                productTaxRate = orderItem.productTaxRate,
//                productTaxableAmount = orderItem.productTaxableAmount,
//                productTaxDetails = orderItem.productTaxDetails
//            )
//        }
//
//        // Get correctly calculated discounted prices for receipt
//        val receiptCartItems = pricingCalculationUseCase.calculateDiscountedPricesForReceipt(
//            cartItems = cartItems,
//            useCardPricing = !isCashPayment
//        )
//
//        return buildString {
//            val receiptWidth = 48
//            val divider = "-".repeat((receiptWidth - 4)) + "\n"
//
//            append("\n")
//
//            append(centerAlign(store?.name ?: getStoreDetailsFromLocalUseCase()?.name ?: "") + "\n")
//            append(
//                centerAlign(
//                    store?.location ?: getStoreDetailsFromLocalUseCase()?.location ?: ""
//                ) + "\n\n"
//            )
//
//            append("Order No   : ${order.orderNumber?.take(30) ?: ""}\n")
//            append(
//                String.format(
//                    "%-20s %15s\n",
//                    "Date : $date",
//                    "Time : $time"
//                )
//            )
//
//            append("\n")
//            append(formatBoxedTotalForPrint(order.total ?: 0.0, isReceiptForRefund))
//
//            val receiptTitle = if (isReceiptForRefund) "REFUND RECEIPT" else "RECEIPT"
//            append("\n\n${centerAlign(receiptTitle)}\n\n")
//
//            append(
//                String.format(
//                    "%-26s %12s\n",
//                    "Items", "Price"
//                )
//            )
//            append(createStraightLine())
//
//            // Use the receipt cart items with correct discounted prices
//            receiptCartItems
//                .groupBy {
//                    val product = it.productId
//                    val variant = it.productVariantId
//                    if (isReceiptForRefund) {
//                        if (variant != null) "variant:$variant" else "product:$product"
//                    } else
//                        if (variant != null) "variant:$variant" else "product:$product"
//
//                }
//                .map { (_, items) ->
//                    items.first().copy(
//                        quantity = items.sumOf { it.quantity ?: 0 }
//                    )
//                }
//                .forEach { item ->
//                    val price = item.price ?: 0.0
//                    val quantity = item.quantity?.toDouble() ?: 0.0
//                    val isDiscounted = item.isDiscounted
//                    val discountedPrice = item.discountPrice
//
//                    // Check for product-level discounts (fixed discount or percentage discount)
//                    val hasProductLevelDiscount =
//                        item.fixedDiscount != null && item.fixedDiscount > 0.0 ||
//                                item.fixedPercentageDiscount != null && item.fixedPercentageDiscount > 0.0
//
//                    val effectivePrice = price
//                    val effectiveTotal = price * quantity
//
//                    // Show product-level discounts with strikethrough
//                    if (hasProductLevelDiscount) {
//                        val productNameString = item.name ?: "-"
//                        val parts = productNameString.split("\n", limit = 2)
//                        val mainProductName = parts[0].trim()
//
//                        // Show original price with strikethrough
//                        append(
//                            formatReceiptEntry(
//                                mainProductName,
//                                effectiveTotal,
//                                true,
//                                isReceiptForRefund
//                            )
//                        )
//                        if (parts.size > 1) {
//                            append(
//                                String.format(
//                                    "%-26s\n", // Indent variant slightly
//                                    parts[1].trim()
//                                )
//                            )
//                        } else {
//                            append(
//                                String.format(
//                                    "%-26s\n",
//                                    item.productVariantName?.let { "Variant: $it" } ?: "-",
//                                )
//                            )
//                        }
//                        // (quantity x price) WITHOUT strikethrough
//                        if (quantity > 1) {
//
//                            append(
//                                String.format(
//                                    "%-26s\n",
//                                    "(${quantity} x ${String.format("$%.2f", effectivePrice)})"
//                                )
//                            )
//
//                            /*if (isReceiptForRefund) {
//                                append(
//                                    String.format(
//                                        "%-26s\n",
//                                        "(${quantity} x ${String.format("-$%.2f", effectivePrice)})"
//                                    )
//                                )
//                            } else {
//                                append(
//                                    String.format(
//                                        "%-26s\n",
//                                        "(${quantity} x ${String.format("$%.2f", effectivePrice)})"
//                                    )
//                                )
//                            }*/
//                        }
//
//                        // Show discount line with discounted price
//                        val discountType = when {
//                            item.fixedPercentageDiscount != null && item.fixedPercentageDiscount > 0.0 ->
//                                "Discount ${item.fixedPercentageDiscount}% off"
//
//                            item.fixedDiscount != null && item.fixedDiscount > 0.0 ->
//                                "Discount $${item.fixedDiscount * quantity} off"
//
//                            else -> "Discount applied"
//                        }
//
//                        // Calculate the actual discounted total (e.g., $89.95 * 0.95 = $85.45)
//                        val actualDiscountedTotal: Double = if (!isCashPayment) {
//                            (item.discountPrice ?: price) * quantity
//                        } else {
//                            // Calculate the discounted price correctly for cash payments
//                            val originalTotal = price * quantity
//                            val discountAmount = when {
//                                item.fixedPercentageDiscount != null && item.fixedPercentageDiscount > 0.0 -> {
//                                    originalTotal * (item.fixedPercentageDiscount / 100.0)
//                                }
//
//                                item.fixedDiscount != null && item.fixedDiscount > 0.0 -> {
//                                    item.fixedDiscount * quantity
//                                }
//
//                                else -> 0.0
//                            }
//                            originalTotal - discountAmount
//                        }
//
//                        append(
//                            formatReceiptEntry(
//                                discountType,
//                                actualDiscountedTotal,
//                                isReceiptForRefund = isReceiptForRefund,
//                            )
//                        )
//                        append(divider)
//                    } else if (isDiscounted && discountedPrice != null) {
//
//                        val effectiveDiscountedTotal =
//                            if (isReceiptForRefund) discountedPrice * quantity else discountedPrice * quantity
//
//                        val formattedEffectivePrice =
//                            String.format("%6.2f", effectivePrice).applyStrikethrough()
//                        val formattedEffectiveTotal =
//                            String.format("%12.2f", effectiveTotal).applyStrikethrough()
//
//                        val productNameString = item.name ?: "-"
//                        val parts =
//                            productNameString.split("\n", limit = 2)
//                        val mainProductName = parts[0].trim()
//
//                        append(
//                            formatReceiptEntry(
//                                mainProductName,
//                                effectiveDiscountedTotal,
//                                isReceiptForRefund = isReceiptForRefund,
//                            )
//                        )
//                        if (parts.size > 1)
//                            append(
//                                String.format(
//                                    "%-26s\n", // Indent variant slightly
//                                    parts[1].trim()
//                                )
//                            )
//                        else {
//                            append(
//                                String.format(
//                                    "%-26s\n",
//                                    item.productVariantName?.let { "Variant: $it" } ?: "-",
//                                )
//                            )
//                        }
//                        // (quantity x price)
//                        if (quantity > 1) {
//                            append(
//                                String.format(
//                                    "%-26s\n",
//                                    "(${quantity} x $formattedEffectivePrice)"
//                                )
//                            )
//
//                            /*if (isReceiptForRefund) {
//                                append(
//                                    String.format(
//                                        "%-26s\n",
//                                        "(${quantity} x $formattedEffectivePrice)"
//                                    )
//                                )
//                            } else {
//                                append(
//                                    String.format(
//                                        "%-26s\n",
//                                        "(${quantity} x $formattedEffectivePrice)"
//                                    )
//                                )
//                            }*/
//                        }
//                        append(divider)
//                    } else {
//                        // Regular item without discounts
//                        val productNameString = item.name ?: "-"
//                        val parts =
//                            productNameString.split("\n", limit = 2)
//                        val mainProductName = parts[0].trim()
//
//                        append(
//                            formatReceiptEntry(
//                                mainProductName,
//                                effectiveTotal,
//                                isReceiptForRefund = isReceiptForRefund
//                            )
//                        )
//                        if (parts.size > 1)
//                            append(
//                                String.format(
//                                    "%-26s\n", // Indent variant slightly
//                                    parts[1].trim()
//                                )
//                            )
//                        else {
//                            append(
//                                String.format(
//                                    "%-26s\n",
//                                    item.productVariantName?.let { "Variant: $it" } ?: "-",
//                                )
//                            )
//                        }
//                        // (quantity x price) without strikethrough
//                        if (quantity > 1) {
//                            append(
//                                String.format(
//                                    "%-26s\n",
//                                    "(${quantity} x ${String.format("$%.2f", effectivePrice)})"
//                                )
//                            )
//                        }
//                        append(divider)
//                    }
//                }
//
//            val subtotal = order.subTotal ?: 0.0
//            val tax = order.taxValue ?: 0.0
//            val discount = order.discountAmount ?: 0.0
//            val total = order.total ?: 0.0
//
//            val totalAmountLabel =
//                if (isReceiptForRefund) "TOTAL REFUND AMOUNT:" else "TOTAL AMOUNT:"
//
//            if (discount != 0.0) {
//                append("\n")
//                if (isReceiptForRefund) {
//                    append(
//                        String.format(
//                            "%-25s %14s\n",
//                            "DISCOUNT:",
//                            String.format("-$%.2f", discount)
//                        )
//                    )
//                } else {
//                    append(
//                        String.format(
//                            "%-25s %14s\n",
//                            "DISCOUNT:",
//                            String.format("-$%.2f", discount)
//                        )
//                    )
//                }
//            }
//            append("\n")
//            append(
//                String.format(
//                    "%-29s %10s\n", "SUBTOTAL:",
//                    if (isReceiptForRefund) String.format("-$%.2f", subtotal) else
//                        String.format("$%.2f", subtotal)
//                )
//            )
//
//            // Enhanced tax breakdown display - aggregate and show ALL taxes once
//            if (tax > 0.0) {
//                append("\n")
//                append("TAX BREAKDOWN:\n")
//
//                // Aggregate all taxes (both store and product level) by description
//                val taxMap = mutableMapOf<String, Double>()
//
//                // Add store-level taxes from order.taxDetails (default only)
//                order.taxDetails?.filter { it.isDefault == true }?.forEach { taxDetail ->
//                    val taxAmount = taxDetail.amount ?: when (taxDetail.type?.lowercase()) {
//                        "percentage" -> {
//                            val rate = taxDetail.value / 100.0
//                            subtotal * rate
//                        }
//                        "fixed amount" -> taxDetail.value
//                        else -> {
//                            val rate = taxDetail.value / 100.0
//                            subtotal * rate
//                        }
//                    }
//
//                    val taxValue = taxDetail.value
//                    val taxType = taxDetail.type ?: "Percentage"
//                    val taxDescription = when (taxType.lowercase()) {
//                        "percentage" -> "${taxDetail.title} ($taxValue%)"
//                        "fixed amount" -> "${taxDetail.title} ($$taxValue)"
//                        else -> "${taxDetail.title} ($taxValue%)"
//                    }
//                    taxMap[taxDescription] = (taxMap[taxDescription] ?: 0.0) + taxAmount
//                }
//
//                // Add product-level taxes from orderItems.appliedTaxes
//                orderItems.forEach { orderItem ->
//                    orderItem.appliedTaxes?.forEach { productTaxDetail ->
//                        val productTaxAmount = productTaxDetail.amount ?: when (productTaxDetail.type?.lowercase()) {
//                            "percentage" -> {
//                                val rate = productTaxDetail.value / 100.0
//                                (orderItem.price?.toDoubleOrNull() ?: 0.0) * rate
//                            }
//                            "fixed amount" -> productTaxDetail.value
//                            else -> {
//                                val rate = productTaxDetail.value / 100.0
//                                (orderItem.price?.toDoubleOrNull() ?: 0.0) * rate
//                            }
//                        }
//
//                        val productTaxValue = productTaxDetail.value
//                        val productTaxType = productTaxDetail.type ?: "Percentage"
//                        val productTaxDescription = when (productTaxType.lowercase()) {
//                            "percentage" -> "${productTaxDetail.title} ($productTaxValue%)"
//                            "fixed amount" -> "${productTaxDetail.title} ($$productTaxValue)"
//                            else -> "${productTaxDetail.title} ($productTaxValue%)"
//                        }
//                        taxMap[productTaxDescription] = (taxMap[productTaxDescription] ?: 0.0) + productTaxAmount
//                    }
//                }
//
//                // Display all taxes together
//                if (taxMap.isNotEmpty()) {
//                    taxMap.forEach { (description, amount) ->
//                        val formattedAmount = if (isReceiptForRefund) String.format("-$%.2f", amount)
//                                              else String.format("$%.2f", amount)
//                        append(formatTaxLineWithWrapping("$description:", formattedAmount))
//                    }
//                } else {
//                    // Fallback to simple tax display
//                    val storeData = getStoreDetailsFromLocalUseCase()
//                    val taxRate = storeData?.taxValue ?: 0.0
//                    val taxDescription = if (taxRate > 0.0) "Tax ($taxRate%)" else "Tax"
//
//                    append(
//                        String.format(
//                            "%-25s %14s\n",
//                            "  $taxDescription:",
//                            if (isReceiptForRefund) String.format("-$%.2f", tax)
//                            else String.format("$%.2f", tax)
//                        )
//                    )
//                }
//            } else {
//                // No tax case
//                append("\n")
//                append(
//                    String.format(
//                        "%-25s %14s\n",
//                        "TAX:",
//                        if (isReceiptForRefund) String.format("-$%.2f", tax) else String.format(
//                            "$%.2f",
//                            tax
//                        )
//                    )
//                )
//            }
//
//            // Combine reward discount and order-level discount into a single DISCOUNT line
//
//            append("\n")
//            append(divider)
//            append(
//                String.format(
//                    "%-25s %14s\n",
//                    totalAmountLabel,
//                    if (isReceiptForRefund) String.format("-$%.2f", total) else String.format(
//                        "$%.2f",
//                        total
//                    )
//                )
//            )
//            append(divider)
//            append(
//                String.format(
//                    "%-25s %14s\n", order.paymentMethod?.uppercase(Locale.getDefault())
//                        ?: "-",
//                    if (isReceiptForRefund)
//                        String.format("-$%.2f", total)
//                    else String.format("$%.2f", total)
//                )
//            )
//            append("\n")
//
//            order.customer?.let { customer ->
//
//                customer.phoneNumber?.let {
//
//                    val redeemedPoints = order.redeemPoints ?: 0
//                    val balancePoints = customer.pointsEarned?.toInt() ?: 0
//
//                    append(divider)
//                    append("REWARD MEMBER DETAILS:")
//                    append("\n\n")
//                    if (redeemedPoints > 0) {
//                        append(
//                            String.format(
//                                "%-25s %14s\n",
//                                "Redeemed Points :",
//                                "$redeemedPoints PTS"
//                            )
//                        )
//                    }
//                    append(String.format("%-25s %14s\n", "Balance :", "$balancePoints PTS"))
//                    append(divider)
//                    append("\n")
//
//                }
//
//            }
//
//            storePolicy.split(" ")
//                .chunked(5)
//                .joinToString("\n") { line ->
//                    centerAlign(line.joinToString(" "))
//                }
//                .let { append(it) }
//                .append("\n")
//
//        }
//    }
//
//    @SuppressLint("DefaultLocale")
//    private fun formatReceiptEntry(
//        mainProductName: String,
//        effectiveTotal: Double,
//        isStrikethrough: Boolean = false,
//        isReceiptForRefund: Boolean,
//    ): String {
//        val productNameColWidth = 30
//        val totalColWidth = 8
//
//        val formattedOutput = StringBuilder()
//        val formattedTotal = if (isStrikethrough) {
//            String.format("$%.2f", effectiveTotal).applyStrikethrough()
//        } else {
//            if (isReceiptForRefund) String.format(
//                "-$%.2f",
//                effectiveTotal
//            ) else String.format("$%.2f", effectiveTotal)
//        }
//
//        if (mainProductName.length <= productNameColWidth) {
//            formattedOutput.append(
//                String.format(
//                    "%-${productNameColWidth}s %${totalColWidth}s\n",
//                    mainProductName,
//                    formattedTotal
//                )
//            )
//        } else {
//            var firstLineName: String
//            var remainingName: String
//
//            var splitPoint = mainProductName.lastIndexOf(" ", productNameColWidth)
//            if (splitPoint == -1 || splitPoint < (productNameColWidth / 2)) {
//                splitPoint = productNameColWidth
//            }
//
//            firstLineName = mainProductName.substring(0, splitPoint).trim()
//            remainingName = mainProductName.substring(splitPoint).trim()
//
//            formattedOutput.append(
//                String.format(
//                    "%-${productNameColWidth}s %${totalColWidth}s\n",
//                    firstLineName,
//                    formattedTotal
//                )
//            )
//            formattedOutput.append(String.format("%-${productNameColWidth}s\n", remainingName))
//        }
//
//        return formattedOutput.toString()
//    }
//
//    private fun centerAlign(text: String): String {
//        val receiptWidth = 40
//        if (text.length >= receiptWidth) {
//            return text
//        }
//
//        val totalPadding = receiptWidth - text.length
//        val leftPadding = totalPadding / 2
//        val rightPadding = totalPadding - leftPadding
//
//        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding)
//    }
//
//    @SuppressLint("DefaultLocale")
//    private fun formatBoxedTotalForPrint(total: Double, isReceiptForRefund: Boolean): String {
//        val totalWidth = 40
//        val innerWidth = totalWidth - 2
//
//        val totalLabel = "TOTAL"
//        val formattedAmount = String.format("$%.2f", total)
//        val displayAmount = if (isReceiptForRefund) "-$formattedAmount" else formattedAmount
//
//        val topBorder = "+" + "-".repeat(innerWidth) + "+"
//        val bottomBorder = "+" + "-".repeat(innerWidth) + "+"
//
//        val totalLabelPadding = (innerWidth - totalLabel.length) / 2
//        val totalLabelLine = "|" + " ".repeat(totalLabelPadding) + totalLabel +
//                " ".repeat(innerWidth - totalLabel.length - totalLabelPadding) + "|"
//
//        val amountPadding = (innerWidth - displayAmount.length) / 2
//        val amountLine =
//            "|" + " ".repeat(amountPadding) +
//                    displayAmount +
//                    " ".repeat(
//                        innerWidth - displayAmount.length - amountPadding
//                    ) + "|"
//
//        return "$topBorder\n$totalLabelLine\n$amountLine\n$bottomBorder"
//    }
//
//    private fun createStraightLine(): String {
//        val totalWidth = 40
//        return "─".repeat(totalWidth - 4) + "\n"
//    }
//
//    /**
//     * Formats a tax line with proper alignment:
//     * - Tax name starts on the left
//     * - Tax value aligned to the right
//     * - If tax name is too long, it wraps to next line but value stays on the right
//     *
//     * Receipt width: 40 characters
//     * Format: "Tax Name:                  $10.00"
//     */
//    private fun formatTaxLineWithWrapping(taxName: String, taxValue: String): String {
//        val receiptWidth = 40
//        val valueWidth = taxValue.length
//        val maxNameWidth = receiptWidth - valueWidth - 1 // -1 for space between name and value
//
//        return if (taxName.length <= maxNameWidth) {
//            // Tax name fits on one line - use standard formatting
//            val padding = receiptWidth - taxName.length - valueWidth
//            String.format("%s%s%s\n", taxName, " ".repeat(padding), taxValue)
//        } else {
//            // Tax name is too long - wrap to next line
//            // Print name on first line, value on same line if possible, otherwise on second line
//            val wrappedName = wrapText(taxName, receiptWidth - valueWidth - 1)
//            val lines = wrappedName.split("\n")
//
//            if (lines.size == 1) {
//                // Name fits on one line after wrapping
//                val padding = receiptWidth - lines[0].length - valueWidth
//                String.format("%s%s%s\n", lines[0], " ".repeat(padding), taxValue)
//            } else {
//                // Name takes multiple lines - put value on the last line
//                val result = StringBuilder()
//                for (i in 0 until lines.size - 1) {
//                    result.append(lines[i]).append("\n")
//                }
//                val lastLine = lines.last()
//                val padding = receiptWidth - lastLine.length - valueWidth
//                result.append(String.format("%s%s%s\n", lastLine, " ".repeat(padding), taxValue))
//                result.toString()
//            }
//        }
//    }
//
//    /**
//     * Wraps text to fit within specified width
//     */
//    private fun wrapText(text: String, maxWidth: Int): String {
//        if (text.length <= maxWidth) return text
//
//        val result = StringBuilder()
//        var currentLine = ""
//        val words = text.split(" ")
//
//        for (word in words) {
//            if (currentLine.isEmpty()) {
//                currentLine = word
//            } else if ((currentLine.length + 1 + word.length) <= maxWidth) {
//                currentLine += " $word"
//            } else {
//                result.append(currentLine).append("\n")
//                currentLine = word
//            }
//        }
//
//        if (currentLine.isNotEmpty()) {
//            result.append(currentLine)
//        }
//
//        return result.toString()
//    }
//
//}