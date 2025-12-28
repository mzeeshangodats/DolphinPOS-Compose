package com.retail.dolphinpos.data.entities.refund

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.retail.dolphinpos.data.entities.transaction.PaymentMethodConverter

@Entity(tableName = "refunds")
@TypeConverters(PaymentMethodConverter::class, RefundStatusConverter::class, RefundTypeConverter::class)
data class RefundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "refund_id")
    val refundId: String, // Unique refund identifier (e.g., "REFUND_S1L1R1U1-1234567890")
    
    @ColumnInfo(name = "order_id")
    val orderId: Long, // Reference to orders table
    
    @ColumnInfo(name = "order_no")
    val orderNo: String, // Order number for quick lookup
    
    @ColumnInfo(name = "refund_type")
    val refundType: RefundType, // FULL or PARTIAL
    
    @ColumnInfo(name = "refund_amount")
    val refundAmount: Double,
    
    @ColumnInfo(name = "refunded_items")
    val refundedItems: String, // JSON string of List<RefundedItem>
    
    @ColumnInfo(name = "payment_method")
    val paymentMethod: com.retail.dolphinpos.data.entities.transaction.PaymentMethod,
    
    @ColumnInfo(name = "refund_status")
    val refundStatus: RefundStatus, // PENDING or SYNCED
    
    @ColumnInfo(name = "server_id")
    val serverId: Int? = null, // Server refund ID (from API)
    
    @ColumnInfo(name = "store_id")
    val storeId: Int,
    
    @ColumnInfo(name = "location_id")
    val locationId: Int,
    
    @ColumnInfo(name = "user_id")
    val userId: Int,
    
    @ColumnInfo(name = "batch_no")
    val batchNo: String? = null,
    
    @ColumnInfo(name = "reason")
    val reason: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

