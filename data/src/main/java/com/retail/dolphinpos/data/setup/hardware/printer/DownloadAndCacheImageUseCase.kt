package com.lingeriepos.common.usecases.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.lingeriepos.preferences.getStringInSharedPreference
import com.lingeriepos.preferences.saveStringInSharedPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject


class DownloadAndUpdateCachedImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREF_LOGO_URL = "cached_logo_url"
        const val APP_LOGO_FILE_NAME = "downloaded_app_logo"
    }

    suspend operator fun invoke(imageUrl: String?): Bitmap? = withContext(Dispatchers.IO) {
        try {

            if(imageUrl == null)
                return@withContext null


            val localFile = File(context.filesDir, APP_LOGO_FILE_NAME)
            val cachedUrl = context.getStringInSharedPreference(PREF_LOGO_URL)

            if (cachedUrl != imageUrl) {
                Log.d("LogoUpdate", "Logo URL changed, updating cache.")
                localFile.delete()
                context.saveStringInSharedPreference(PREF_LOGO_URL, imageUrl)
                return@withContext downloadAndSaveImage(imageUrl)
            }

            val serverLastModified = getServerLastModified(imageUrl)
            if (shouldUpdateImage(localFile, serverLastModified)) {
                return@withContext downloadAndSaveImage(imageUrl)
            }

            loadBitmapFromLocalStorage()
        } catch (e: Exception) {
            e.printStackTrace()
            loadBitmapFromLocalStorage()
        }
    }

    private suspend fun getServerLastModified(imageUrl: String): Long = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = URL(imageUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connect()
            connection.lastModified
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun shouldUpdateImage(localFile: File, serverLastModified: Long): Boolean {
        if (!localFile.exists()) return true
        return serverLastModified > localFile.lastModified()
    }

    private suspend fun loadBitmapFromLocalStorage(): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, APP_LOGO_FILE_NAME)
        return@withContext if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    private suspend fun downloadAndSaveImage(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)

                val file = File(context.filesDir, APP_LOGO_FILE_NAME)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                file.setLastModified(System.currentTimeMillis())
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

