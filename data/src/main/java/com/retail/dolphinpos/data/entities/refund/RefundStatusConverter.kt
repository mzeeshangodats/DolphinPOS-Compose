package com.retail.dolphinpos.data.entities.refund

import androidx.room.TypeConverter

class RefundStatusConverter {
    @TypeConverter
    fun fromRefundStatus(status: RefundStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toRefundStatus(value: String): RefundStatus {
        return RefundStatus.valueOf(value)
    }
}

