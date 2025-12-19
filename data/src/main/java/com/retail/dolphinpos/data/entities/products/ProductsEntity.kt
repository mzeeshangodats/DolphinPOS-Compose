package com.retail.dolphinpos.data.entities.products

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "server_id") val serverId: Int? = null, // Server product ID (from API)
    val categoryId: Int, // Keep existing column name
    val storeId: Int, // Keep existing column name
    val name: String?,
    val description: String?,
    val quantity: Int,
    val status: String?,
    val cashPrice: String, // Keep existing column name
    val cardPrice: String, // Keep existing column name
    val price: String? = null, // Base price
    @ColumnInfo(name = "compare_at_price") val compareAtPrice: String? = null,
    @ColumnInfo(name = "cost_price") val costPrice: String? = null,
    val barCode: String?, // Keep existing column name
    @ColumnInfo(name = "secondary_bar_codes") val secondaryBarCodes: String? = null, // JSON string
    val chargeTaxOnThisProduct: Boolean?, // Keep existing column name
    val locationId: Int, // Keep existing column name
    val cardTax: Double = 0.0, // Keep existing column name
    val cashTax: Double = 0.0, // Keep existing column name
    @ColumnInfo(name = "track_quantity") val trackQuantity: Boolean = false,
    @ColumnInfo(name = "continue_selling_when_out_of_stock") val continueSellingWhenOutOfStock: Boolean = false,
    @ColumnInfo(name = "product_vendor_id") val productVendorId: Int? = null,
    @ColumnInfo(name = "current_vendor_id") val currentVendorId: Int? = null,
    @ColumnInfo(name = "attributes") val attributes: String? = null, // JSON string Map<String, String>
    @ColumnInfo(name = "sales_channel") val salesChannel: String? = null, // JSON string List<String>
    @ColumnInfo(name = "shipping_weight") val shippingWeight: Double? = null,
    @ColumnInfo(name = "shipping_weight_unit") val shippingWeightUnit: String? = null,
    @ColumnInfo(name = "is_physical_product") val isPhysicalProduct: Boolean = true,
    @ColumnInfo(name = "customs_information") val customsInformation: String? = null,
    @ColumnInfo(name = "is_ebt_eligible") val isEBTEligible: Boolean = false,
    @ColumnInfo(name = "is_id_required") val isIDRequired: Boolean = false,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false, // Sync status for offline-first
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
