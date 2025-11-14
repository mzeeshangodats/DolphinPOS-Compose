package com.retail.dolphinpos.data.entities.report

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch_report")
data class BatchReportEntity(
    @PrimaryKey
    val batchNo: String,
    val closed: String?, // JSON string
    val closedBy: Int,
    val closingCashAmount: Double,
    val closingTime: String?,
    val createdAt: String?,
    val id: Int,
    val locationId: Int,
    val openTime: String?,
    val opened: String?, // JSON string
    val openedBy: Int,
    val payInCard: String?, // JSON string for Any type
    val payInCash: String?, // JSON string for Any type
    val payOutCard: String?, // JSON string for Any type
    val payOutCash: String?, // JSON string for Any type
    val startingCashAmount: Double,
    val status: String?,
    val storeId: Int,
    val storeRegisterId: Int,
    val totalAbandonOrders: Int,
    val totalAmount: String?,
    val totalCardAmount: String?,
    val totalCashAmount: String?,
    val totalCashDiscount: String?,
    val totalDiscount: String?,
    val totalOnlineSales: String?,
    val totalPayIn: String?, // JSON string for Any type
    val totalPayOut: String?, // JSON string for Any type
    val totalRewardDiscount: String?,
    val totalSales: String?, // JSON string for Any type
    val totalTax: String?,
    val totalTip: Int,
    val totalTipCard: Int,
    val totalTipCash: Int,
    val totalTransactions: Int,
    val updatedAt: String?
)

