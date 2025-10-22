package com.retail.dolphinpos.data.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageDownloadService @Inject constructor(
    private val context: Context
) {

    private val imagesDir = File(context.filesDir, "cached_images")

    init {
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
    }

    suspend fun downloadImage(imageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val fileName = generateFileName(imageUrl)
            val file = File(imagesDir, fileName)

            // Download the image
            url.openStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            return@withContext file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun isImageCached(localPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(localPath)
            return@withContext file.exists() && file.length() > 0
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun deleteImage(localPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(localPath)
            return@withContext file.delete()
        } catch (e: Exception) {
            return@withContext false
        }
    }

    private fun generateFileName(imageUrl: String): String {
        val urlHash = imageUrl.hashCode().toString()
        val extension = getFileExtension(imageUrl)
        return "img_${urlHash}.${extension}"
    }

    private fun getFileExtension(url: String): String {
        return try {
            val path = URL(url).path
            val extension = path.substringAfterLast('.', "")
            if (extension.isNotEmpty()) extension else "jpg"
        } catch (e: Exception) {
            "jpg"
        }
    }

    fun getFileSize(localPath: String): Long {
        return try {
            val file = File(localPath)
            if (file.exists()) file.length() else 0
        } catch (e: Exception) {
            0
        }
    }
}
