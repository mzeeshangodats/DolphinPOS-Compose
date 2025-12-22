package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.TypeConverters

@Entity(tableName = "batch")
@TypeConverters(BatchSyncStatusConverter::class)
data class BatchEntity(
    @PrimaryKey(autoGenerate = true)
    val batchId: Int = 0,
    /**
     * Client-generated batch ID (e.g., "BATCH_S1L1R1U1-1234567890")
     * This is used as the canonical ID for sync operations
     */
    val batchNo: String,
    val userId: Int?,
    val storeId: Int?,
    val registerId: Int?,
    val locationId: Int?,
    val startingCashAmount: Double,
    val startedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val closingCashAmount: Double? = null,
    
    /**
     * Sync status tracking for offline-first sync system
     * Defaults to ACTIVE_LOCAL when batch is created
     */
    @ColumnInfo(name = "sync_status")
    val syncStatus: BatchSyncStatus = BatchSyncStatus.ACTIVE_LOCAL,
    
    /**
     * Legacy field - kept for backward compatibility
     * Use syncStatus instead for new code
     */
    @Deprecated("Use syncStatus instead")
    val isSynced: Boolean = false
)
