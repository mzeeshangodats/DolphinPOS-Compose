package com.retail.dolphinpos.data.entities.user

import androidx.room.TypeConverter

/**
 * TypeConverter for Room to handle BatchLifecycleStatus enum.
 */
class BatchLifecycleStatusConverter {
    @TypeConverter
    fun fromBatchLifecycleStatus(status: BatchLifecycleStatus): String {
        return status.name
    }

    @TypeConverter
    fun toBatchLifecycleStatus(name: String): BatchLifecycleStatus {
        return BatchLifecycleStatus.valueOf(name)
    }
}

