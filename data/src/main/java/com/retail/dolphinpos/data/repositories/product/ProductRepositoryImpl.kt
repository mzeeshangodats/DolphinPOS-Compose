package com.retail.dolphinpos.data.repositories.product

import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.entities.category.CategoryEntity
import com.retail.dolphinpos.data.entities.products.ProductImagesEntity
import com.retail.dolphinpos.data.entities.products.ProductsEntity
import com.retail.dolphinpos.data.entities.products.VariantImagesEntity
import com.retail.dolphinpos.data.entities.products.VariantsEntity
import com.retail.dolphinpos.data.mapper.ProductMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.product.CreateProductRequest
import com.retail.dolphinpos.domain.model.product.FileUploadResponse
import com.retail.dolphinpos.domain.model.product.ProductImageRequest
import com.retail.dolphinpos.domain.model.product.VendorListResponse
import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ProductRepositoryImpl(
    private val productsDao: ProductsDao,
    private val apiService: ApiService,
    private val gson: Gson
) : ProductRepository {

    override suspend fun createProduct(request: CreateProductRequest): Result<Long> {
        return try {
            // Convert CreateProductRequest to ProductsEntity
            val productEntity = ProductMapper.toProductEntityFromRequest(request)
            
            // Insert product (returns Long for auto-generated ID)
            val productId = productsDao.insertProduct(productEntity)
            
            // Insert product images
            val productImages = request.images.map { image ->
                ProductImagesEntity(
                    productId = productId.toInt(),
                    fileURL = image.fileURL,
                    originalName = image.originalName
                )
            }
            if (productImages.isNotEmpty()) {
                productsDao.insertProductImages(productImages)
            }
            
            // Insert variants and their images
            val variantEntities = request.variants.map { variant ->
                ProductMapper.toVariantEntityFromRequest(variant, productId.toInt(), gson)
            }
            if (variantEntities.isNotEmpty()) {
                variantEntities.forEachIndexed { index, variantEntity ->
                    val variantId = productsDao.insertVariant(variantEntity)
                    
                    // Insert variant images if available
                    val variant = request.variants[index]
                    if (variant.images != null && (variant.images as Collection<Any?>).isNotEmpty()) {
                        val variantImages = (variant.images as Iterable<ProductImageRequest?>).map { image ->
                            VariantImagesEntity(
                                variantId = variantId.toInt(),
                                fileURL = image?.fileURL,
                                originalName = image?.originalName
                            )
                        }
                        productsDao.insertVariantImages(variantImages)
                    }
                }
            }
            
            Result.success(productId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncProductToServer(productId: Long): Result<Int> {
        return try {
            val productEntity = productsDao.getProductByLocalId(productId.toInt())
                ?: return Result.failure(Exception("Product not found"))
            
            // Get related data
            val productImages = productsDao.getProductImagesByProductId(productId.toInt())
            val variants = productsDao.getVariantsByProductId(productId.toInt())
            
            // Get variant images for all variants
            val variantImagesMap = variants.associate { variant ->
                val images = productsDao.getVariantImagesByVariantId(variant.id)
                variant.id to images
            }
            
            // Convert to CreateProductRequest
            val request = ProductMapper.toCreateProductRequest(productEntity, productImages, variants, variantImagesMap, gson)
            
            // Call API
            val result = safeApiCallResult(
                apiCall = { apiService.createProduct(request) },
                defaultMessage = "Product creation failed"
            )
            
            // Mark as synced on success
            result.onSuccess { response ->
                // Update product with server ID and mark as synced
                val serverId = response.id
                val updatedProduct = productEntity.copy(
                    isSynced = true,
                    serverId = serverId,
                    updatedAt = System.currentTimeMillis()
                )
                productsDao.updateProduct(updatedProduct)
                
                // Mark variants as synced
                // Note: If API returns variant IDs, update them accordingly
                variants.forEach { variant ->
                    val updatedVariant = variant.copy(
                        isSynced = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    // Note: Variant server IDs would come from API response if available
                }
            }
            
            result.map { it.id } // Return server product ID
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFiles(files: List<File>, type: String): Result<FileUploadResponse> {
        return try {
            val fileParts = files.map { file ->
                val mimeType = getMimeType(file)
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", file.name, requestFile)
            }
            val typeBody = type
                .toRequestBody("text/plain".toMediaType())

            safeApiCallResult(
                apiCall = { apiService.uploadFiles(fileParts, typeBody) },
                defaultMessage = "File upload failed"
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getVendors(): Result<VendorListResponse> {
        return safeApiCallResult(
            apiCall = { 
                apiService.getVendors(
                    paginate = false,
                    page = 1,
                    orderBy = "createdAt",
                    order = "DESC"
                )
            },
            defaultMessage = "Failed to fetch vendors"
        )
    }

    override suspend fun getCategories(): List<com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData> {
        val categoryEntities = productsDao.getCategories()
        return ProductMapper.toCategory(categoryEntities)
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/jpeg" // Default to JPEG if unknown
        }
    }

//    override suspend fun getUnsyncedProducts(): List<Long> {
//        return productsDao.getUnsyncedProducts().map { it.id.toLong() }
//    }
}

