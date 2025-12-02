package com.retail.dolphinpos.data.entities.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tax_details",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["locationId"])]
)
data class TaxDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "locationId")
    val locationId: Int,
    val type: String?,
    val title: String?,
    val value: Double,
    val amount: Double? = null,
    @ColumnInfo(name = "isDefault")
    val isDefault: Boolean? = false,
    @ColumnInfo(name = "refundedTax")
    val refundedTax: Double? = null
)

