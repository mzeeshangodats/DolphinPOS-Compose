package com.retail.dolphinpos.data.entities.refund

import androidx.room.TypeConverter

class RefundTypeConverter {
    @TypeConverter
    fun fromRefundType(type: RefundType): String {
        return type.name
    }
    
    @TypeConverter
    fun toRefundType(value: String): RefundType {
        return RefundType.valueOf(value)
    }
}

