package com.retail.dolphinpos.data.entities.order

import androidx.room.TypeConverter

class OrderSyncStatusConverter {
    
    @TypeConverter
    fun fromOrderSyncStatus(status: OrderSyncStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toOrderSyncStatus(value: String): OrderSyncStatus {
        return OrderSyncStatus.valueOf(value)
    }
}

