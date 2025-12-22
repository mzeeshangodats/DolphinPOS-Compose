package com.retail.dolphinpos.data.entities.user

import androidx.room.TypeConverter

class BatchSyncStatusConverter {
    
    @TypeConverter
    fun fromBatchSyncStatus(status: BatchSyncStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toBatchSyncStatus(value: String): BatchSyncStatus {
        return BatchSyncStatus.valueOf(value)
    }
}

