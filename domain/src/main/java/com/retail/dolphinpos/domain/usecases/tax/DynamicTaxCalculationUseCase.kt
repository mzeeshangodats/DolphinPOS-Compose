package com.retail.dolphinpos.domain.usecases.tax

import com.retail.dolphinpos.domain.model.home.create_order.TaxDetail
import javax.inject.Inject

data class TaxCalculationResult(
    val cardTax: Double,
    val cashTax: Double
)

class DynamicTaxCalculationUseCase @Inject constructor() {

    /**
     * Calculate tax dynamically based on discounted prices and tax details from Room database
     * 
     * @param cardPrice Discounted card price
     * @param cashPrice Discounted cash price
     * @param taxDetails List of tax details from Room database (filtered by locationId and isDefault = true)
     * @param chargeTaxOnThisProduct Whether tax should be applied to this product
     * @return TaxCalculationResult with calculated cardTax and cashTax
     */
    fun calculateTax(
        cardPrice: Double,
        cashPrice: Double,
        taxDetails: List<TaxDetail>?,
        chargeTaxOnThisProduct: Boolean? = true
    ): TaxCalculationResult {
        // If product is tax exempt, return zero tax
        if (chargeTaxOnThisProduct == false) {
            return TaxCalculationResult(cardTax = 0.0, cashTax = 0.0)
        }

        // If no tax details, return zero tax
        if (taxDetails.isNullOrEmpty()) {
            return TaxCalculationResult(cardTax = 0.0, cashTax = 0.0)
        }

        // Filter tax details to only include active taxes (isDefault = true)
        val activeTaxDetails = taxDetails.filter { it.isDefault == true }

        // Calculate card tax
        val cardTax = calculateTaxForPrice(cardPrice, activeTaxDetails)

        // Calculate cash tax
        val cashTax = calculateTaxForPrice(cashPrice, activeTaxDetails)

        return TaxCalculationResult(cardTax = cardTax, cashTax = cashTax)
    }

    /**
     * Calculate tax for a specific price based on tax details
     * 
     * @param price The price to calculate tax on (after discount)
     * @param taxDetails List of active tax details
     * @return Total tax amount
     */
    private fun calculateTaxForPrice(price: Double, taxDetails: List<TaxDetail>): Double {
        if (price <= 0.0 || taxDetails.isEmpty()) {
            return 0.0
        }

        var totalTax = 0.0

        taxDetails.forEach { taxDetail ->
            val taxAmount = when (taxDetail.type?.lowercase()) {
                "percentage" -> {
                    // Percentage tax: (price * value) / 100
                    // Example: price = 99, value = 6.25 -> (99 * 6.25) / 100 = 6.1875
                    (price * taxDetail.value) / 100.0
                }
                "fixed amount" -> {
                    // Fixed amount tax: value
                    taxDetail.value
                }
                else -> {
                    // Default to percentage if type is unknown
                    (price * taxDetail.value) / 100.0
                }
            }
            totalTax += taxAmount
        }

        return totalTax
    }
}

