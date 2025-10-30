package com.retail.dolphinpos.data.entities.holdcart

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "hold_cart")
data class HoldCartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @SerializedName("cart_name")
    val cartName: String,
    
    @SerializedName("cart_items")
    val cartItems: String, // JSON string of CartItem list
    
    @SerializedName("subtotal")
    val subtotal: Double,
    
    @SerializedName("tax")
    val tax: Double,
    
    @SerializedName("total_amount")
    val totalAmount: Double,
    
    @SerializedName("cash_discount_total")
    val cashDiscountTotal: Double = 0.0,
    
    @SerializedName("order_discount_total")
    val orderDiscountTotal: Double = 0.0,
    
    @SerializedName("is_cash_selected")
    val isCashSelected: Boolean = false,
    
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("store_id")
    val storeId: Int,
    
    @SerializedName("register_id")
    val registerId: Int
)
