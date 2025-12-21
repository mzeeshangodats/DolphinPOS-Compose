package com.retail.dolphinpos.data.entities.user

import androidx.room.TypeConverter

/**
 * TypeConverter for BatchSyncStatus enum.
 * Allows Room to store enum as String in database.
 * Handles migration from old enum values (SYNC_PENDING, SYNCED, SYNC_FAILED).
 */
class BatchSyncStatusConverter {
    
    @TypeConverter
    fun fromString(value: String?): BatchSyncStatus? {
        return value?.let { 
            try {
                BatchSyncStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                // Handle migration from old enum values
                when (it) {
                    "SYNC_PENDING" -> BatchSyncStatus.START_PENDING
                    "SYNCED" -> BatchSyncStatus.START_SYNCED
                    "SYNC_FAILED" -> BatchSyncStatus.FAILED
                    else -> BatchSyncStatus.START_PENDING // Default fallback
                }
            }
        }
    }
    
    @TypeConverter
    fun toString(status: BatchSyncStatus?): String? {
        return status?.name
    }
}

