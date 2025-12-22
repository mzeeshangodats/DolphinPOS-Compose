package com.retail.dolphinpos.data.entities.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for ensuring only one worker processes the sync queue at a time
 * This table should always contain exactly one row
 */
@Entity(tableName = "sync_lock")
data class SyncLockEntity(
    @PrimaryKey
    val id: Int = 1, // Always 1, only one row exists
    
    /**
     * Worker instance ID holding the lock
     * null if unlocked
     */
    val lockedBy: String? = null,
    
    /**
     * Timestamp when lock was acquired
     */
    val lockedAt: Long? = null
)

