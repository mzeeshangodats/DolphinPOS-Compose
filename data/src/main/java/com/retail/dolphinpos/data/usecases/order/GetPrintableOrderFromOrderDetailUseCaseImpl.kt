package com.retail.dolphinpos.data.usecases.order

import com.retail.dolphinpos.domain.model.home.create_order.CardDetails
import com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem
import com.retail.dolphinpos.domain.model.home.create_order.CheckoutSplitPaymentTransactions
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailList
import com.retail.dolphinpos.domain.model.home.order_details.OrderItem
import com.retail.dolphinpos.domain.model.home.order_details.Transaction
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.domain.usecases.order.GetPrintableOrderFromOrderDetailUseCase
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class GetPrintableOrderFromOrderDetailUseCaseImpl @Inject constructor() : GetPrintableOrderFromOrderDetailUseCase {

    override fun invoke(orderDetail: OrderDetailList): PendingOrder {
        return PendingOrder(
            id = orderDetail.id.toLong(),
            orderNumber = orderDetail.orderNumber,
            invoiceNo = orderDetail.orderNumber,
            customerId = orderDetail.customerId.toSafeInt(),
            storeId = orderDetail.storeId,
            locationId = orderDetail.locationId,
            storeRegisterId = orderDetail.storeRegisterId,
            batchNo = orderDetail.batchId.toStringOrNull(),
            paymentMethod = orderDetail.paymentMethod,
            isRedeemed = orderDetail.isRedeemed,
            source = orderDetail.source,
            redeemPoints = orderDetail.redeemPoints.toSafeInt(),
            items = orderDetail.orderItems.map(::mapOrderItem),
            subTotal = orderDetail.subTotal.toCleanDouble() ?: 0.0,
            total = orderDetail.total.toCleanDouble() ?: 0.0,
            applyTax = orderDetail.applyTax,
            taxValue = orderDetail.taxValue,
            discountAmount = orderDetail.discountAmount.toCleanDouble() ?: 0.0,
            cashDiscountAmount = orderDetail.cashDiscountAmount.toSafeDouble() ?: 0.0,
            rewardDiscount = orderDetail.rewardDiscount.toSafeDouble() ?: 0.0,
            discountIds = null,
            transactionId = orderDetail.transactions.firstOrNull()?.invoiceNo,
            userId = orderDetail.cashierId.toSafeInt() ?: 0,
            voidReason = orderDetail.voidReason?.toStringOrNull(),
            isVoid = false,//orderDetail.isVoid,
            transactions = orderDetail.transactions.map(::mapTransaction).ifEmpty { null },
            cardDetails = orderDetail.transactions.firstOrNull()?.cardDetails,
            createdAt = orderDetail.createdAt.toEpochMillis(),
            isSynced = true
        )
    }

    private fun mapOrderItem(orderItem: OrderItem): CheckOutOrderItem {
        val price = orderItem.price.toCleanDouble()
        val discountedPrice = orderItem.discountedPrice.toCleanDouble()
        val discountAmount = if (price != null && discountedPrice != null) {
            (price - discountedPrice).coerceAtLeast(0.0)
        } else null

        return CheckOutOrderItem(
            productId = orderItem.product.id,
            quantity = orderItem.quantity,
            productVariantId = null,
            name = orderItem.product.name,
            isCustom = false,
            price = price,
            barCode = null,
            reason = null,
            discountId = null,
            discountedPrice = discountedPrice,
            discountedAmount = discountAmount,
            fixedDiscount = discountAmount,
            discountReason = null,
            fixedPercentageDiscount = discountAmount?.let { if (price != null && price != 0.0) (it / price) * 100 else null },
            discountType = if (discountAmount != null && discountAmount > 0) "fixed" else null,
            cardPrice = price
        )
    }

    private fun mapTransaction(transaction: Transaction): CheckoutSplitPaymentTransactions {
        val amount = transaction.amount.toCleanDouble() ?: 0.0
        return CheckoutSplitPaymentTransactions(
            invoiceNo = transaction.invoiceNo,
            paymentMethod = transaction.paymentMethod,
            amount = amount,
            cardDetails = transaction.cardDetails,
            baseAmount = amount,
            taxAmount = transaction.tax,
            dualPriceAmount = null
        )
    }

    private fun Any?.toStringOrNull(): String? = when (this) {
        null -> null
        is String -> this.ifBlank { null }
        else -> this.toString()
    }

    private fun Any?.toSafeInt(): Int? = when (this) {
        null -> null
        is Number -> this.toInt()
        is String -> this.toCleanDouble()?.toInt()
        else -> this.toString().toCleanDouble()?.toInt()
    }

    private fun Any?.toSafeDouble(): Double? = when (this) {
        null -> null
        is Number -> this.toDouble()
        is String -> this.toCleanDouble()
        else -> this.toString().toCleanDouble()
    }

    private fun String.toCleanDouble(): Double? {
        val sanitized = this.replace("$", "").replace(",", "").trim()
        return sanitized.toDoubleOrNull()
    }

    private fun String.toEpochMillis(): Long {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val formatter = SimpleDateFormat(pattern, Locale.US)
                formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return formatter.parse(this)?.time ?: continue
            } catch (_: ParseException) {
            }
        }
        return System.currentTimeMillis()
    }
}


