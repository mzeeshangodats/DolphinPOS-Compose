package com.retail.dolphinpos.data.entities.transaction

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "transactions")
@TypeConverters(PaymentMethodConverter::class)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "order_no")
    val orderNo: String? = null,
    
    @ColumnInfo(name = "order_id")
    val orderId: Int? = null,
    
    @ColumnInfo(name = "store_id")
    val storeId: Int? = null,
    
    @ColumnInfo(name = "location_id")
    val locationId: Int? = null,
    
    @ColumnInfo(name = "payment_method")
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    
    val status: String = "pending", // ENUM: 'pending', 'paid', 'failed', 'void', 'refund', 'settled'
    
    val amount: Double, // DECIMAL(10, 2)
    
    @ColumnInfo(name = "invoice_no")
    val invoiceNo: String? = null, // STRING(100), unique
    
    @ColumnInfo(name = "batch_id")
    val batchId: Int? = null,
    
    @ColumnInfo(name = "batch_no")
    val batchNo: String? = null,
    
    @ColumnInfo(name = "user_id")
    val userId: Int? = null,
    
    @ColumnInfo(name = "order_source")
    val orderSource: String? = null, // ENUM: 'register', 'online', 'store'
    
    val tax: Double? = null, // FLOAT
    
    val tip: Double? = null, // FLOAT

    @ColumnInfo(name = "card_details")
    val cardDetails: String? = null, // JSON stored as String
    
    @ColumnInfo(name = "tax_details")
    val taxDetails: String? = null, // JSON string of List<TaxDetail>
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
