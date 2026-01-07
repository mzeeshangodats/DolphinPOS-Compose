package com.retail.dolphinpos.data.datasource

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object ExternalStorageHelper {
    private const val BACKUP_FILE_NAME = "dolphin_db_backup.db"
    private const val BACKUP_MIME_TYPE = "application/x-sqlite3"



    /**
     * Check if backup file exists
     */
    fun checkIfFileExists(): Boolean {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            BACKUP_FILE_NAME
        )

        return if (file.exists()) {
            true
        } else {
            false
        }
    }



    /**
     * Save backup file to Downloads folder
     */
    suspend fun saveBackupToDownloads(context: Context, sourceFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Use MediaStore API (no permission needed)
                saveViaMediaStore(context, sourceFile)
            } else {
                // Android 9 and below - Use File API
                saveViaFile(context, sourceFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    private suspend fun saveViaMediaStore(context: Context, sourceFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            deleteExistingBackup(contentResolver, collection)

            // Step 2: Create new file entry with exact filename
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, BACKUP_FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, BACKUP_MIME_TYPE)
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
                )
                put(MediaStore.Downloads.IS_PENDING, 0)
            }


            val uri = contentResolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create backup file"))

            // Step 3: Write file content
            contentResolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    private fun deleteExistingBackup(
        resolver: ContentResolver,
        collection: Uri
    ) {
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val args = arrayOf(BACKUP_FILE_NAME)

        resolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            selection,
            args,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val uri = ContentUris.withAppendedId(collection, id)
                resolver.delete(uri, null, null)
            }
        }
    }

    private suspend fun saveViaFile(context: Context, sourceFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Step 1: Delete all existing backup files (with and without extension, and numbered versions)
            downloadsDir.listFiles()?.forEach { file ->
                val fileName = file.name.lowercase()
                // Delete if filename contains "dolphin_db_backup" (matches base name and numbered versions)
                if (fileName.contains("dolphin_db_backup")) {
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        // Ignore delete errors
                    }
                }
            }

            // Step 2: Create new backup file with exact filename
            val backupFile = File(downloadsDir, BACKUP_FILE_NAME)

            // Step 3: Copy source file to Downloads
            sourceFile.inputStream().use { input ->
                backupFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

