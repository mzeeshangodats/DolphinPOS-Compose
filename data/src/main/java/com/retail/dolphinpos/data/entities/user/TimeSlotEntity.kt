package com.retail.dolphinpos.data.entities.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_slots")
data class TimeSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val slug: String,
    val storeId: Int,
    val time: String,
    val userId: Int,
    val isSynced: Boolean = false
)


