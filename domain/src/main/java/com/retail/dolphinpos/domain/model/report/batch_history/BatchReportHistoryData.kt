package com.retail.dolphinpos.domain.model.report.batch_history

import com.retail.dolphinpos.domain.model.report.batch_report.Opened

data class BatchReportHistoryData(
    val batchNo: String,
    val closed: Any,
    val closedBy: Any,
    val closingCashAmount: Any,
    val closingTime: Any,
    val createdAt: String,
    val id: Int,
    val locationId: Int,
    val openTime: String,
    val opened: Opened,
    val openedBy: Int,
    val startingCashAmount: Double,
    val status: String,
    val storeId: Int,
    val storeRegisterId: Int,
    val totalDiscount: String,
    val totalSales: Any,
    val totalTax: Any,
    val totalTransactions: Int,
    val updatedAt: String
)