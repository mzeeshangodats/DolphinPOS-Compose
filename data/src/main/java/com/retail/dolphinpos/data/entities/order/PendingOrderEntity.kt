package com.retail.dolphinpos.data.entities.order

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_orders")
data class PendingOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "order_no") val orderNo: String,
    @ColumnInfo(name = "customer_id") val customerId: Int? = null,
    @ColumnInfo(name = "store_id") val storeId: Int,
    @ColumnInfo(name = "store_register_id") val storeRegisterId: Int? = null,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "is_redeemed") val isRedeemed: Boolean = false,
    val source: String = "point-of-sale",
    @ColumnInfo(name = "redeem_points") val redeemPoints: Int? = null,
    @ColumnInfo(name = "order_items") val items: String, // JSON string of List<CheckOutOrderItem>
    @ColumnInfo(name = "sub_total") val subTotal: Double,
    val total: Double,
    @ColumnInfo(name = "apply_tax") val applyTax: Boolean = true,
    @ColumnInfo(name = "tax_value") val taxValue: Double,
    @ColumnInfo(name = "discount_amount") val discountAmount: Double,
    @ColumnInfo(name = "cash_discount_amount") val cashDiscountAmount: Double,
    @ColumnInfo(name = "reward_discount") val rewardDiscount: Double,
    @ColumnInfo(name = "discount_ids") val discountIds: String? = null, // JSON string of List<Int>
    @ColumnInfo(name = "transaction_id") val transactionId: String? = null,
    @ColumnInfo(name = "batch_id") val batchId: Int = 0,
    @ColumnInfo(name = "cashier_id") val cashierId: Int = 0,
    @ColumnInfo(name = "void_reason") val voidReason: String? = null,
    @ColumnInfo(name = "is_void") val isVoid: Boolean = false,
    val transactions: String? = null, // JSON string
    @ColumnInfo(name = "card_details") val cardDetails: String? = null, // JSON string
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)

