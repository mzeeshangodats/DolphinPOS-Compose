package com.retail.dolphinpos.data.entities.transaction

enum class PaymentMethod(val value: String) {
    CASH("cash"),
    CARD("card"),
    GIFT_CARD("gift-card"),
    SPLIT("split");

    companion object {
        fun fromString(value: String): PaymentMethod {
            return values().find { it.value == value } ?: CASH
        }
    }
}

