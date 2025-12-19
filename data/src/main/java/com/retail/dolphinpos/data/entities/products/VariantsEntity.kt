package com.retail.dolphinpos.data.entities.products

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "variants",
    foreignKeys = [
        ForeignKey(
            entity = ProductsEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VariantsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "server_id") val serverId: Int? = null, // Server variant ID (from API)
    val productId: Int, // Keep existing column name
    @ColumnInfo(name = "card_price") val cardPrice: String?,
    @ColumnInfo(name = "cash_price") val cashPrice: String?,
    val price: String? = null, // Base price
    @ColumnInfo(name = "cost_price") val costPrice: String? = null,
    val quantity: Int,
    val sku: String?,
    val plu: String?,
    val title: String?,
    @ColumnInfo(name = "bar_code") val barCode: String?=null,
    @ColumnInfo(name = "attributes") val attributes: String? = null, // JSON string Map<String, String>
    @ColumnInfo(name = "location_id") val locationId: Int? = null,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false, // Sync status for offline-first
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
