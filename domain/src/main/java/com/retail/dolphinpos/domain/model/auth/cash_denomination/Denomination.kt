package com.retail.dolphinpos.domain.model.auth.cash_denomination

data class Denomination(
    val value: Double,
    var count: Int = 0,
    val label: String,
    val type: DenominationType = DenominationType.CASH
) {
    val subtotal: Double
        get() = value * count

    fun updateCount(newCount: Int): Denomination {
        return copy(count = newCount)
    }
}

enum class DenominationType {
    CASH, COIN
}
