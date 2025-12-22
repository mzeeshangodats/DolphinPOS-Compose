package com.retail.dolphinpos.data.entities.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for generating monotonic sequence numbers for sync commands
 * This table should always contain exactly one row
 */
@Entity(tableName = "sync_sequence")
data class SyncSequenceEntity(
    @PrimaryKey
    val id: Int = 1, // Always 1, only one row exists
    
    /**
     * Current sequence number (monotonic increasing)
     */
    val currentSequence: Long = 0
)

