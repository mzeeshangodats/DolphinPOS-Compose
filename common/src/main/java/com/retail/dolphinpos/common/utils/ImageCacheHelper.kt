package com.retail.dolphinpos.common.utils

import android.content.Context
import com.retail.dolphinpos.domain.repositories.auth.StoreRegistersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCacheHelper @Inject constructor(
    private val context: Context,
    private val storeRegistersRepository: StoreRegistersRepository
) {
    
    /**
     * Get the appropriate image path for display
     * Returns cached local path if available, otherwise returns original URL
     */
    suspend fun getImagePath(imageUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val cachedPath = storeRegistersRepository.getCachedImagePath(imageUrl)
            cachedPath ?: imageUrl
        } catch (e: Exception) {
            imageUrl
        }
    }
    
    /**
     * Check if an image is cached locally
     */
    suspend fun isImageCached(imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            storeRegistersRepository.getCachedImagePath(imageUrl) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get product images with local paths (if cached)
     */
    suspend fun getProductImagesWithLocalPaths(productId: Int) = withContext(Dispatchers.IO) {
        try {
            storeRegistersRepository.getProductImagesWithLocalPaths(productId)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get variant images with local paths (if cached)
     */
    suspend fun getVariantImagesWithLocalPaths(variantId: Int) = withContext(Dispatchers.IO) {
        try {
            storeRegistersRepository.getVariantImagesWithLocalPaths(variantId)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
