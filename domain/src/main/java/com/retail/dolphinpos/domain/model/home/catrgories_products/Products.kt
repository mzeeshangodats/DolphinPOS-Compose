package com.retail.dolphinpos.domain.model.home.catrgories_products

import com.retail.dolphinpos.domain.model.TaxDetail

data class Products(
    val barCode: String?,
    val cardPrice: String,
    val cashPrice: String,
    val categoryId: Int,
    val chargeTaxOnThisProduct: Boolean?,
    val compareAtPrice: String? = "",
    val continueSellingWhenOutOfStock: Boolean? = false,
    val costPrice: String? = "",
    val createdAt: String? = "",
    val currentVendorId: Int = 0,
    val description: String?,
    val id: Int,
    val images: List<ProductImage>?,
    val isEBTEligible: Boolean = false,
    val isHSTEligible: Boolean = false,
    val isIDRequired: Boolean = false,
    val isProductBarCode: Boolean = false,
    val locationId: Int,
    val name: String?,
    val price: String? = "",
    val quantity: Int,
    val cardTax: Double,
    val cashTax: Double,
    val secondaryBarcodes: List<SecondaryBarcode>?,
    val status: String?,
    val storeId: Int,
    val trackQuantity: Boolean = false,
    val updatedAt: String? = "",
    val variants: List<Variant>?,
    val vendor: Vendor?,
    val taxAmount: Double? = null,
    val taxDetails: List<TaxDetail>? = null
)