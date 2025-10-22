package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch")
data class BatchEntity(
    @PrimaryKey(autoGenerate = true)
    val batchId: Int = 0,
    val batchNo: String,
    val userId: Int?,
    val storeId: Int?,
    val registerId: Int?,
    val startingCashAmount: Double,
    val startedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val closingCashAmount: Double? = null
)
