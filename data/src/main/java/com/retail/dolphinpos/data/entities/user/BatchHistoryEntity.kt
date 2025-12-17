package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch_history")
data class BatchHistoryEntity(
    @PrimaryKey
    val id: Int,
    val batchNo: String,
    val startingCashAmount: Double,
    val closingCashAmount: Double?,
    val openTime: String,
    val closingTime: String?,
    val status: String,
    val storeId: Int,
    val locationId: Int,
    val storeRegisterId: Int,
    val openedBy: Int,
    val closedBy: Int?,
    val totalSales: String?,
    val totalTax: String?,
    val totalDiscount: String?,
    val totalTransactions: Int,
    val createdAt: String,
    val updatedAt: String
)

