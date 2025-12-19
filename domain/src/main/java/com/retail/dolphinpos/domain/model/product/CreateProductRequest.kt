package com.retail.dolphinpos.domain.model.product

data class CreateProductRequest(
    val name: String,
    val description: String,
    val images: List<ProductImageRequest>,
    val status: String = "active",
    val price: String? = null,
    val costPrice: String? = null,
    val trackQuantity: Boolean = true,
    val quantity: Int = 0,
    val continueSellingWhenOutOfStock: Boolean = false,
    val salesChannel: List<String> = emptyList(),
    val productVendorId: Int? = null,
    val currentVendorId: Int? = null,
    val categoryId: Int,
    val storeId: Int,
    val locationId: Int,
    val shippingWeight: Double? = null,
    val shippingWeightUnit: String? = null,
    val isPhysicalProduct: Boolean = true,
    val customsInformation: String? = null,
    val barCode: String? = null,
    val secondaryBarCodes: List<String> = emptyList(),
    //val varients: Map<String, List<String>> = emptyMap(), // Note: API uses "varients" not "variants"
    val cashPrice: String? = null,
    val cardPrice: String? = null,
    val variants: List<ProductVariantRequest> = emptyList()
)

data class ProductImageRequest(
    val fileURL: String,
    val originalName: String
)

data class ProductVariantRequest(
    val title: String,
    val price: String? = null,
    val costPrice: String? = null,
    val quantity: Int = 0,
    val barCode: String? = null,
    val sku: String? = null,
    val cashPrice: String? = null,
    val cardPrice: String? = null,
    val locationId: Int,
    val images: List<ProductImageRequest>? = null
)

