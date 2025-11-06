package com.retail.dolphinpos.data.entities.transaction

import androidx.room.TypeConverter

class PaymentMethodConverter {
    @TypeConverter
    fun fromPaymentMethod(paymentMethod: PaymentMethod): String {
        return paymentMethod.value
    }

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod {
        return PaymentMethod.fromString(value)
    }
}

