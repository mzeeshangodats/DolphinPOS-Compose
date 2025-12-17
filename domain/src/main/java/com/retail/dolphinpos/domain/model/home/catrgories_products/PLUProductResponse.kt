package com.retail.dolphinpos.domain.model.home.catrgories_products

import com.google.gson.annotations.SerializedName

data class PLUProductResponse(
    val success: Boolean,
    val message: String? = null,
    @SerializedName("data")
    val data: PLUProductData?
)

data class PLUProductData(
    val products: List<PLUProduct>?,
    val pagination: Pagination?,
    val filters: Filters?
)

data class PLUProduct(
    val id: Int,
    val name: String?,
    val description: String?,
    val status: String?,
    val price: String?,
    val compareAtPrice: String?,
    val costPrice: String?,
    val trackQuantity: Boolean?,
    val availableOnAllStore: Boolean?,
    val isCustom: Boolean?,
    val quantity: Int?,
    val continueSellingWhenOutOfStock: Boolean?,
    val isProductBarCode: Boolean?,
    val barCode: String?,
    val plu: String?,
    val salesChannel: List<String>?,
    val productVendorId: Int?,
    val currentVendorId: Int?,
    val categoryId: Int?,
    val storeId: Int?,
    val locationId: Int?,
    val cashPrice: String?,
    val cardPrice: String?,
    val isEBTEligible: Boolean?,
    val isIDRequired: Boolean?,
    val chargeTaxOnThisProduct: Boolean?,
    val isHSTEligible: Boolean?,
    val taxDetails: List<com.retail.dolphinpos.domain.model.home.create_order.TaxDetail>?,
    val category: CategoryInfo?,
    val vendor: VendorInfo?,
    val variants: List<Any>?,
    val images: List<ImageInfo>?,
    val discountedPrice: String?,
    val isDiscounted: Boolean?,
    val discountApplied: Any?,
    val totalPrice: Double?,
    val taxAmount: Double?,
    val variantOptions: Map<String, List<String>>?,
    val actualVariants: List<Any>?,
    val variantStats: Any?
)

data class CategoryInfo(
    val id: Int?,
    val title: String?,
    val description: String?
)

data class VendorInfo(
    val id: Int?,
    val title: String?
)

data class ImageInfo(
    val fileURL: String?,
    val originalName: String?
)

data class Pagination(
    val totalRecords: Int?,
    val currentPage: Int?,
    val totalPages: Int?,
    val hasNextPage: Boolean?,
    val hasPrevPage: Boolean?
)

data class Filters(
    val storeId: String?,
    val locationId: String?,
    val status: String?,
    val keyword: String?,
    val barCode: String?
)

