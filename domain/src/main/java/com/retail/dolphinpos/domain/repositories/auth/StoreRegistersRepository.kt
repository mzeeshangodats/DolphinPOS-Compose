package com.retail.dolphinpos.domain.repositories.auth

import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.domain.model.auth.logout.LogoutResponse
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.UpdateStoreRegisterData
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.UpdateStoreRegisterResponse
import com.retail.dolphinpos.domain.model.auth.select_registers.request.UpdateStoreRegisterRequest
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.ProductImage
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.ProductsResponse
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant
import com.retail.dolphinpos.domain.model.home.catrgories_products.VariantImage
import com.retail.dolphinpos.domain.model.home.catrgories_products.Vendor

interface StoreRegistersRepository {
    suspend fun updateStoreRegister(updateStoreRegisterRequest: UpdateStoreRegisterRequest): UpdateStoreRegisterResponse
    suspend fun getProducts(storeID: Int, locationID: Int): ProductsResponse
    suspend fun logout(): LogoutResponse
    suspend fun getLocations(storeID: Int): List<Locations>
    suspend fun getRegistersByLocationID(locationID: Int): List<Registers>
    suspend fun insertRegisterStatusDetailsIntoLocalDB(updateStoreRegisterData: UpdateStoreRegisterData)
    suspend fun getRegisterStatus(): UpdateStoreRegisterData
    suspend fun insertCategoriesIntoLocalDB(categoryList: List<CategoryData>)
    suspend fun insertProductsIntoLocalDB(productList: List<Products>, categoryId: Int)
    suspend fun insertProductImagesIntoLocalDB(productImageList: List<ProductImage>?, productId: Int)
    suspend fun insertProductVariantsIntoLocalDB(productVariantList: List<Variant>, productId: Int)
    suspend fun insertVariantImagesIntoLocalDB(variantImageList: List<VariantImage>, variantId: Int)
    suspend fun insertVendorDetailsIntoLocalDB(vendor: Vendor, productId: Int)
    
    // Image downloading methods
    suspend fun downloadAndCacheImages(imageUrls: List<String>)
    suspend fun getCachedImagePath(imageUrl: String): String?
    suspend fun clearOldCachedImages()
    
    // Product image methods with local paths
    suspend fun getProductImagesWithLocalPaths(productId: Int): List<ProductImage>
    suspend fun getVariantImagesWithLocalPaths(variantId: Int): List<VariantImage>

}