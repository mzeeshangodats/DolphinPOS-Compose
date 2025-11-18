package com.retail.dolphinpos.data.entities.order

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Order identification
    @ColumnInfo(name = "order_no") val orderNumber: String,
    @ColumnInfo(name = "invoice_no") val invoiceNo: String? = null,
    @ColumnInfo(name = "server_id") val serverId: Int? = null,  // Server order ID (from API)
    
    // Customer & Store info
    @ColumnInfo(name = "customer_id") val customerId: Int? = null,
    @ColumnInfo(name = "store_id") val storeId: Int,
    @ColumnInfo(name = "location_id") val locationId: Int,
    @ColumnInfo(name = "store_register_id") val storeRegisterId: Int? = null,
    @ColumnInfo(name = "batch_no") val batchNo: String? = null,
    
    // Payment info
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "transaction_id") val transactionId: String? = null,
    
    // Order items (JSON)
    @ColumnInfo(name = "order_items") val items: String, // JSON string of List<CheckOutOrderItem>
    
    // Pricing
    @ColumnInfo(name = "sub_total") val subTotal: Double,
    val total: Double,
    @ColumnInfo(name = "apply_tax") val applyTax: Boolean = true,
    @ColumnInfo(name = "tax_value") val taxValue: Double,
    @ColumnInfo(name = "discount_amount") val discountAmount: Double = 0.0,
    @ColumnInfo(name = "cash_discount_amount") val cashDiscountAmount: Double = 0.0,
    @ColumnInfo(name = "reward_discount") val rewardDiscount: Double = 0.0,
    
    // Status & Sync
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,  // 1 = completed/synced, 0 = pending/unsynced
    @ColumnInfo(name = "order_source") val orderSource: String,     // "api" or "local"
    @ColumnInfo(name = "status") val status: String? = null,        // "completed" or "pending"
    
    // Other fields
    @ColumnInfo(name = "is_redeemed") val isRedeemed: Boolean = false,
    val source: String = "point-of-sale",
    @ColumnInfo(name = "redeem_points") val redeemPoints: Int? = null,
    @ColumnInfo(name = "discount_ids") val discountIds: String? = null, // JSON string of List<Int>
    @ColumnInfo(name = "split_transactions") val transactions: String? = null, // JSON string of List<CheckoutSplitPaymentTransactions>
    @ColumnInfo(name = "card_details") val cardDetails: String? = null, // JSON string of CardDetails
    @ColumnInfo(name = "tax_details") val taxDetails: String? = null, // JSON string of List<TaxDetail>
    @ColumnInfo(name = "user_id") val userId: Int = 0,
    @ColumnInfo(name = "void_reason") val voidReason: String? = null,
    @ColumnInfo(name = "is_void") val isVoid: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

