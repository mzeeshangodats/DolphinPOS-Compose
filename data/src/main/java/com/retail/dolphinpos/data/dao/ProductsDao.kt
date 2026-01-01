package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
    suspend fun insertProduct(product: ProductsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductImages(productImagesList: List<ProductImagesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(variantsList: List<VariantsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: VariantsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariantImages(variantsImagesList: List<VariantImagesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendor(vendorEntity: VendorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendors(vendorList: List<VendorEntity>)

    @Query("SELECT * FROM vendor WHERE productId = 0 ORDER BY title ASC")
    suspend fun getVendorsList(): List<VendorEntity>

    @Query("SELECT * FROM category")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM products ORDER BY created_at DESC")
    suspend fun getAllProducts(): List<ProductsEntity>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId")
    suspend fun getProductsByCategoryID(categoryId: Int?): List<ProductsEntity>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Int): ProductsEntity?

    @Query("SELECT * FROM products WHERE server_id = :serverId LIMIT 1")
    suspend fun getProductByServerId(serverId: Int): ProductsEntity?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barCode LIKE '%' || :query || '%'")
    suspend fun searchProducts(query: String): List<ProductsEntity>

    @Query("SELECT * FROM products WHERE barCode = :barcode LIMIT 1")
    suspend fun searchProductByBarcode(barcode: String): ProductsEntity?

    @Query("SELECT * FROM products WHERE plu = :plu AND storeId = :storeId AND locationId = :locationId LIMIT 1")
    suspend fun searchProductByPLU(plu: String, storeId: Int, locationId: Int): ProductsEntity?

    @Query("SELECT * FROM variants WHERE sku = :sku LIMIT 1")
    suspend fun searchVariantBySku(sku: String): VariantsEntity?

    @Query("SELECT * FROM variants WHERE plu = :plu LIMIT 1")
    suspend fun searchVariantByPLU(plu: String): VariantsEntity?

    @Query("SELECT * FROM variants WHERE server_id = :serverId LIMIT 1")
    suspend fun getVariantByServerId(serverId: Int): VariantsEntity?

    @Query("SELECT * FROM variants WHERE sku LIKE '%' || :query || '%'")
    suspend fun searchVariantsBySku(query: String): List<VariantsEntity>

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

    @Query("DELETE FROM product_images WHERE productId = :productId")
    suspend fun deleteProductImagesByProductId(productId: Int)

    @Query("SELECT * FROM variant_images WHERE variantId = :variantId")
    suspend fun getVariantImagesByVariantId(variantId: Int): List<VariantImagesEntity>

    @Query("SELECT * FROM variants WHERE productId = :productId")
    suspend fun getVariantsByProductId(productId: Int): List<VariantsEntity>

    // Delete methods for cleanup before sync
    @Query("DELETE FROM category")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("DELETE FROM product_images")
    suspend fun deleteAllProductImages()

    @Query("DELETE FROM variants")
    suspend fun deleteAllVariants()

    @Query("DELETE FROM variant_images")
    suspend fun deleteAllVariantImages()

    @Query("DELETE FROM vendor")
    suspend fun deleteAllVendors()

    // Update quantity methods
    @Query("UPDATE products SET quantity = quantity - :quantityToDeduct WHERE id = :productId")
    suspend fun deductProductQuantity(productId: Int, quantityToDeduct: Int)

    @Query("UPDATE variants SET quantity = quantity - :quantityToDeduct WHERE id = :variantId")
    suspend fun deductVariantQuantity(variantId: Int, quantityToDeduct: Int)

    // Product creation and sync methods
    @Update
    suspend fun updateProduct(product: ProductsEntity)

//    @Query("SELECT * FROM products WHERE is_synced = 0 ORDER BY createdAt ASC")
//    suspend fun getUnsyncedProducts(): List<ProductsEntity>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductByLocalId(productId: Int): ProductsEntity?

    @Query("SELECT * FROM variants WHERE productId = :productId AND is_synced = 0")
    suspend fun getUnsyncedVariantsByProductId(productId: Int): List<VariantsEntity>

    @Query("DELETE FROM variants WHERE productId = :productId")
    suspend fun deleteVariantsByProductId(productId: Int)

//    @Query("UPDATE products SET is_synced = 1, server_id = :serverId, updatedAt = :updatedAt WHERE id = :productId")
//    suspend fun markProductAsSynced(productId: Int, serverId: Int, updatedAt: Long)

    @Query("UPDATE variants SET is_synced = 1, server_id = :serverId, updated_at = :updatedAt WHERE id = :variantId")
    suspend fun markVariantAsSynced(variantId: Int, serverId: Int, updatedAt: Long)

}