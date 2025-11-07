package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.retail.dolphinpos.data.entities.category.CategoryEntity
import com.retail.dolphinpos.data.entities.products.CachedImageEntity
import com.retail.dolphinpos.data.entities.products.ProductImagesEntity
import com.retail.dolphinpos.data.entities.products.ProductsEntity
import com.retail.dolphinpos.data.entities.products.VariantImagesEntity
import com.retail.dolphinpos.data.entities.products.VariantsEntity
import com.retail.dolphinpos.data.entities.products.VendorEntity

@Dao
interface ProductsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categoryList: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(productList: List<ProductsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductImages(productImagesList: List<ProductImagesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(variantsList: List<VariantsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariantImages(variantsImagesList: List<VariantImagesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendor(vendorEntity: VendorEntity)

    @Query("SELECT * FROM category")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductsEntity>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId")
    suspend fun getProductsByCategoryID(categoryId: Int?): List<ProductsEntity>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%'")
    suspend fun searchProducts(query: String): List<ProductsEntity>

    @Query("SELECT * FROM products WHERE barCode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductsEntity?

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Int): ProductsEntity?

    @Query("SELECT * FROM variants WHERE barCode = :barcode LIMIT 1")
    suspend fun getVariantByBarcode(barcode: String): VariantsEntity?

    // Cached Images methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedImage(cachedImage: CachedImageEntity)

    @Query("SELECT * FROM cached_images WHERE originalUrl = :url")
    suspend fun getCachedImage(url: String): CachedImageEntity?

    @Query("SELECT * FROM cached_images WHERE originalUrl IN (:urls)")
    suspend fun getCachedImages(urls: List<String>): List<CachedImageEntity>

    @Query("DELETE FROM cached_images WHERE originalUrl = :url")
    suspend fun deleteCachedImage(url: String)

    @Query("DELETE FROM cached_images WHERE downloadedAt < :timestamp")
    suspend fun deleteOldCachedImages(timestamp: Long)

    // Update product image local paths
    @Query("UPDATE product_images SET localPath = :localPath, isCached = :isCached WHERE fileURL = :fileURL")
    suspend fun updateProductImageLocalPath(fileURL: String, localPath: String?, isCached: Boolean)

    @Query("UPDATE variant_images SET localPath = :localPath, isCached = :isCached WHERE fileURL = :fileURL")
    suspend fun updateVariantImageLocalPath(fileURL: String, localPath: String?, isCached: Boolean)

    @Query("SELECT * FROM product_images WHERE productId = :productId")
    suspend fun getProductImagesByProductId(productId: Int): List<ProductImagesEntity>

    @Query("SELECT * FROM variant_images WHERE variantId = :variantId")
    suspend fun getVariantImagesByVariantId(variantId: Int): List<VariantImagesEntity>

    @Query("SELECT * FROM variants WHERE productId = :productId")
    suspend fun getVariantsByProductId(productId: Int): List<VariantsEntity>

}