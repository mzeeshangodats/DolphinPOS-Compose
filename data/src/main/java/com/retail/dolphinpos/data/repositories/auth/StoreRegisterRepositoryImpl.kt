package com.retail.dolphinpos.data.repositories.auth

import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.entities.products.CachedImageEntity
import com.retail.dolphinpos.data.mapper.ProductMapper
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.service.ImageDownloadService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers
import com.retail.dolphinpos.domain.model.auth.logout.LogoutResponse
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.storeRegisters.StoreRegistersResponse
import com.retail.dolphinpos.data.util.safeApiCall
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.VerifyRegisterData
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.VerifyRegisterResponse
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.updateRegister.UpdateStoreRegisterData
import com.retail.dolphinpos.domain.model.auth.select_registers.reponse.updateRegister.UpdateStoreRegisterResponse
import com.retail.dolphinpos.domain.model.auth.select_registers.request.UpdateStoreRegisterRequest
import com.retail.dolphinpos.domain.model.home.catrgories_products.Category
import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Metadata
import com.retail.dolphinpos.domain.model.home.catrgories_products.ProductImage
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products
import com.retail.dolphinpos.domain.model.home.catrgories_products.ProductsResponse
import com.retail.dolphinpos.domain.model.home.catrgories_products.Variant
import com.retail.dolphinpos.domain.model.home.catrgories_products.VariantImage
import com.retail.dolphinpos.domain.model.home.catrgories_products.Vendor
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import retrofit2.HttpException

class StoreRegisterRepositoryImpl(
    private val api: ApiService,
    private val userDao: UserDao,
    private val productsDao: ProductsDao,
    private val imageDownloadService: ImageDownloadService,
    private val gson: Gson
) : StoreRegistersRepository {

    override suspend fun updateStoreRegister(updateStoreRegisterRequest: UpdateStoreRegisterRequest): UpdateStoreRegisterResponse {
        return safeApiCall(
            apiCall = { api.updateStoreRegister(updateStoreRegisterRequest) },
            defaultResponse = {
                UpdateStoreRegisterResponse(
                    message = "Failed to assign register",
                    data = UpdateStoreRegisterData(
                        storeId = 0,
                        locationId = 0,
                        storeRegisterId = 0,
                        status = "",
                        updatedAt = ""
                    )
                )
            }
        )
    }

    override suspend fun verifyStoreRegister(verifyRegisterRequest: com.retail.dolphinpos.domain.model.auth.select_registers.request.VerifyRegisterRequest): com.retail.dolphinpos.domain.model.auth.select_registers.reponse.VerifyRegisterResponse {
        return safeApiCall(
            apiCall = { api.verifyStoreRegister(verifyRegisterRequest) },
            defaultResponse = {
                VerifyRegisterResponse(
                    data = VerifyRegisterData(
                        locationId = 0,
                        name = "",
                        status = "",
                        storeId = 0,
                        storeRegisterId = 0,
                        updatedAt = ""
                    )
                )
            }
        )
    }

    override suspend fun getProducts(storeID: Int, locationID: Int): ProductsResponse {
        return safeApiCall(
            apiCall = { api.getProducts(storeID, locationID) },
            defaultResponse = {
                ProductsResponse(
                    message = "",
                    metadata = Metadata(
                        lastSync = 0,
                        locationId = "",
                        storeId = "",
                        syncTimestamp = "",
                        totalCategories = 0,
                        totalProducts = 0
                    ),
                    success = true,
                    category = Category(
                        categories = emptyList()
                    )
                )
            }
        )
    }

    override suspend fun logout(): LogoutResponse {
        return safeApiCall(
            apiCall = { api.logout() },
            defaultResponse = {
                LogoutResponse(message = "Logout failed")
            }
        )
    }

    override suspend fun getLocations(storeID: Int): List<Locations> {
        val locationEntities = userDao.getLocationsByStoreId(storeID)
        return UserMapper.toLocationsAgainstStoreID(locationEntities, gson)
    }

    override suspend fun getRegistersByLocationID(locationID: Int): List<Registers> {
        return try {
            // Get storeId from database (StoreEntity.id is the store ID)
            val storeEntity = userDao.getStore()
            val storeId = storeEntity.id
            if (storeId == 0) {
                return emptyList()
            }
            
            // Call API to get registers for the selected location
            val response: StoreRegistersResponse = safeApiCall(
                apiCall = { api.getStoreRegisters(storeId, locationID) },
                defaultResponse = {
                    StoreRegistersResponse(
                        data = emptyList()
                    )
                }
            )
            
            // Filter and map only active registers
            response.data
                .filter { it.status.equals("active", ignoreCase = true) }
                .map { storeRegister ->
                    Registers(
                        id = storeRegister.id,
                        name = storeRegister.name,
                        status = storeRegister.status,
                        locationId = storeRegister.locationId
                    )
                }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun insertRegisterIntoLocalDB(register: Registers) {
        try {
            val registerEntity = UserMapper.toRegisterEntity(register)
            userDao.insertRegisters(registerEntity)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun insertRegisterStatusDetailsIntoLocalDB(updateStoreRegisterData: UpdateStoreRegisterData) {
        try {
            userDao.insertRegisterStatusDetails(
                UserMapper.toRegisterStatusEntity(
                    updateStoreRegisterData
                )
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getRegisterStatus(): UpdateStoreRegisterData {
        val registerStatusDetailEntities = userDao.getRegisterStatusDetail()
        return UserMapper.toRegisterStatus(registerStatusDetailEntities)
    }

    override suspend fun insertCategoriesIntoLocalDB(categoryList: List<CategoryData>) {
        try {
            val categoryEntities = categoryList.map { category ->
                ProductMapper.toCategoryEntity(
                    category = category
                )
            }
            productsDao.insertCategories(categoryEntities)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun insertProductsIntoLocalDB(productList: List<Products>, categoryId: Int, storeId: Int, locationId: Int) {
        try {
            val productEntities = productList.map { product ->
                ProductMapper.toProductEntity(
                    products = product,
                    categoryId = categoryId,
                    storeId = storeId,
                    locationId = locationId
                )
            }
            productsDao.insertProducts(productEntities)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun insertProductImagesIntoLocalDB(
        productImageList: List<ProductImage>?,
        productId: Int
    ) {
        try {
            // productId is the server ID, need to find the local product ID
            val productEntity = productsDao.getProductByServerId(productId)
                ?: productsDao.getProductById(productId)
            
            if (productEntity == null) {
                // Product not found, skip image insertion
                return
            }
            
            val localProductId = productEntity.id
            val productImagesEntities = productImageList?.map { productImage ->
                ProductMapper.toProductImagesEntity(
                    productImages = productImage,
                    productId = localProductId
                )
            }
            productImagesEntities?.let { productsDao.insertProductImages(it) }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun insertProductVariantsIntoLocalDB(
        productVariantList: List<Variant>,
        productId: Int
    ) {
        try {
            // productId is the server ID, need to find the local product ID
            val productEntity = productsDao.getProductByServerId(productId)
                ?: productsDao.getProductById(productId)
            
            if (productEntity == null) {
                // Product not found, skip variant insertion
                return
            }
            
            val localProductId = productEntity.id
            val productVariantEntities = productVariantList.map { productVariant ->
                ProductMapper.toProductVariantsEntity(
                    variants = productVariant,
                    productId = localProductId
                )
            }
            productsDao.insertVariants(productVariantEntities)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun insertVariantImagesIntoLocalDB(
        variantImageList: List<VariantImage>,
        variantId: Int
    ) {
        try {
            // variantId is the server ID, need to find the local variant ID
            val variantEntity = productsDao.getVariantByServerId(variantId)
            
            if (variantEntity == null) {
                // Variant not found, skip image insertion
                return
            }
            
            val localVariantId = variantEntity.id
            val variantImagesEntities = variantImageList.map { variantImage ->
                ProductMapper.toVariantImagesEntity(
                    variantImages = variantImage,
                    variantId = localVariantId
                )
            }
            productsDao.insertVariantImages(variantImagesEntities)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun insertVendorDetailsIntoLocalDB(vendor: Vendor, productId: Int) {
        try {
            // productId is the server ID, need to find the local product ID
            val productEntity = productsDao.getProductByServerId(productId)
                ?: productsDao.getProductById(productId)
            
            if (productEntity == null) {
                // Product not found, skip vendor insertion
                return
            }
            
            val localProductId = productEntity.id
            productsDao.insertVendor(
                ProductMapper.toProductVendorEntity(
                    vendor = vendor, productId = localProductId
                )
            )
        } catch (e: Exception) {
            throw e
        }
    }
    
    override suspend fun deleteAllProductsData() {
        try {
            productsDao.deleteAllVariants()
            productsDao.deleteAllVariantImages()
            productsDao.deleteAllProductImages()
            productsDao.deleteAllVendors()
            productsDao.deleteAllProducts()
            productsDao.deleteAllCategories()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun deleteSyncedProductsData() {
        try {
            // Delete variant images for variants of synced products
            productsDao.deleteSyncedVariantImages()
            // Delete product images for synced products
            productsDao.deleteSyncedProductImages()
            // Delete synced products (variants will be deleted via CASCADE foreign key)
            productsDao.deleteSyncedProducts()
            // Note: Categories are not deleted as they may be shared with local products
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun downloadAndCacheImages(imageUrls: List<String>) {
        try {
            coroutineScope {
                // Download images in parallel
                val downloadJobs = imageUrls.map { url ->
                    async {
                        try {
                            // Check if image is already cached
                            val cachedImage = productsDao.getCachedImage(url)
                            if (cachedImage != null && imageDownloadService.isImageCached(
                                    cachedImage.localPath
                                )
                            ) {
                                return@async
                            }

                            // Download the image
                            val localPath = imageDownloadService.downloadImage(url)
                            if (localPath != null) {
                                val fileName = localPath.substringAfterLast("/")
                                val fileSize = imageDownloadService.getFileSize(localPath)
                                
                                val cachedImageEntity = CachedImageEntity(
                                    originalUrl = url,
                                    localPath = localPath,
                                    fileName = fileName,
                                    downloadedAt = System.currentTimeMillis(),
                                    fileSize = fileSize,
                                    isDownloaded = true
                                )
                                
                                productsDao.insertCachedImage(cachedImageEntity)
                                
                                // Update product and variant image entities with local path
                                productsDao.updateProductImageLocalPath(url, localPath, true)
                                productsDao.updateVariantImageLocalPath(url, localPath, true)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Log error but don't fail the entire operation
                        }
                    }
                }

                downloadJobs.awaitAll()
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getCachedImagePath(imageUrl: String): String? {
        return try {
            val cachedImage = productsDao.getCachedImage(imageUrl)
            if (cachedImage != null && imageDownloadService.isImageCached(cachedImage.localPath)) {
                cachedImage.localPath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearOldCachedImages() {
        try {
            // Clear images older than 7 days
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            productsDao.deleteOldCachedImages(sevenDaysAgo)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getProductImagesWithLocalPaths(productId: Int): List<ProductImage> {
        return try {
            val productImageEntities = productsDao.getProductImagesByProductId(productId)
            productImageEntities.map { entity ->
                ProductImage(
                    fileURL = entity.localPath ?: entity.fileURL, // Use local path if available, otherwise original URL
                    originalName = entity.originalName
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVariantImagesWithLocalPaths(variantId: Int): List<VariantImage> {
        return try {
            val variantImageEntities = productsDao.getVariantImagesByVariantId(variantId)
            variantImageEntities.map { entity ->
                VariantImage(
                    fileURL = entity.localPath ?: entity.fileURL, // Use local path if available, otherwise original URL
                    originalName = entity.originalName
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}