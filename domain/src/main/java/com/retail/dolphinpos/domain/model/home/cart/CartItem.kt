package com.retail.dolphinpos.domain.model.home.cart

import com.retail.dolphinpos.domain.model.TaxDetail
import com.retail.dolphinpos.domain.model.home.catrgories_products.VariantImage

data class CartItem(
    val productId: Int?,
    val productVariantId: Int? = null,
    val productVariantName: String? = null,
    val name: String?,
    val cardPrice: Double = 0.0,
    val cashPrice: Double = 0.0,
    val selectedPrice: Double = 0.0,
    var quantity: Int,
    val imageUrl: String?,
    var removeFromCart: Boolean = false,
    var discountPrice: Double? = null,
    var isDiscounted: Boolean = false,
    var isCustom: Boolean = false,
    var reason: String? = null,
    var barCode: String? = null,
    var discountId: Int? = null,
    val images: VariantImage? = null,
    val costPrice: Double? = 0.0,
    val sku: String? = "",
    var chargeTaxOnThisProduct: Boolean? = true,
    val fixedDiscount: Double? = null,
    val discountReason: String? = "",
    val fixedPercentageDiscount: Double? = null,
    val discountType: DiscountType? = null,
    val discountValue: Double? = 0.0,
    var cashDiscountedPrice: Double = 0.0,
    // Tax-related fields for product-level tax calculation
    val productTaxDetails: List<TaxDetail>? = null,
    var productTaxAmount: Double = 0.0,
    var productTaxRate: Double = 0.0,
    var productTaxableAmount: Double = 0.0
)
enum class DiscountType {
    PERCENTAGE, AMOUNT
}

fun CartItem.getProductDiscountPercentage(): Double {
    return when (discountType) {
        DiscountType.PERCENTAGE -> discountValue ?: 0.0
        DiscountType.AMOUNT -> {
            if (selectedPrice > 0) ((discountValue ?: 0.0) / selectedPrice) * 100.0 else 0.0
        }
        else -> 0.0
    }
}


fun CartItem.getProductDiscountedPrice(): Double {
    return when (discountType) {
        DiscountType.PERCENTAGE -> selectedPrice - ((selectedPrice * (discountValue ?: 0.0)) / 100.0)
        DiscountType.AMOUNT -> selectedPrice - (discountValue ?: 0.0)
        else -> selectedPrice
    }
}

fun CartItem.getProductDiscountAmount(): Double {
    return when (discountType) {
        DiscountType.PERCENTAGE -> ((selectedPrice * (discountValue ?: 0.0)) / 100.0)
        DiscountType.AMOUNT -> discountValue ?: 0.0
        else -> 0.0
    }
}

// Extension properties for PricingCalculationUseCase compatibility
val CartItem.applyTax: Boolean
    get() = chargeTaxOnThisProduct ?: true

val CartItem.price: Double?
    get() = cardPrice.takeIf { it > 0.0 }