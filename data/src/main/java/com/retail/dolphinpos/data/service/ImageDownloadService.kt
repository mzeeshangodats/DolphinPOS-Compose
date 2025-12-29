package com.retail.dolphinpos.data.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageDownloadService @Inject constructor(
    private val context: Context
) {

    private val imagesDir = File(context.filesDir, "cached_images")
    
    // OkHttpClient configured for image downloads with proper timeout and retry settings
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    init {
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
    }

    suspend fun downloadImage(imageUrl: String): String? = withContext(Dispatchers.IO) {
        val fileName = generateFileName(imageUrl)
        val file = File(imagesDir, fileName)
        
        try {
            val request = Request.Builder()
                .url(imageUrl)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                // Clean up if file was partially created
                if (file.exists()) {
                    file.delete()
                }
                return@withContext null
            }

            response.body?.let { body ->
                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                
                // Verify file was downloaded successfully
                if (file.exists() && file.length() > 0) {
                    return@withContext file.absolutePath
                } else {
                    // Clean up invalid file
                    file.delete()
                    return@withContext null
                }
            } ?: run {
                return@withContext null
            }
        } catch (e: SocketException) {
            // Connection reset or other socket errors
            cleanupPartialFile(file)
            return@withContext null
        } catch (e: SocketTimeoutException) {
            // Timeout errors
            cleanupPartialFile(file)
            return@withContext null
        } catch (e: IOException) {
            // Network I/O errors (including unexpected end of stream)
            cleanupPartialFile(file)
            return@withContext null
        } catch (e: Exception) {
            // Any other unexpected errors
            cleanupPartialFile(file)
            return@withContext null
        }
    }
    
    private fun cleanupPartialFile(file: File) {
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
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
