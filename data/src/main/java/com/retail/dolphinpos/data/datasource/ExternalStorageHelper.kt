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
    private const val BACKUP_DISPLAY_NAME = "Dolphin POS Database Backup"

    /**
     * Get the backup file path (Downloads folder)
     */
    fun getBackupFilePath(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use MediaStore API
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(downloadsDir, BACKUP_FILE_NAME).absolutePath
        } else {
            // Android 9 and below - Use external storage
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(downloadsDir, BACKUP_FILE_NAME).absolutePath
        }
    }

    /**
     * Check if backup file exists
     */
    suspend fun backupFileExists_old(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Query MediaStore
            val uri = findBackupFileUri(context)
            if (uri != null) {
                true
            } else {
                // Broader scan
                findBackupFileByScanning(context) != null
            }
        } else {
            // Android 9 and below - Check via File
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BACKUP_FILE_NAME)
            file.exists() || File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BACKUP_FILE_NAME.removeSuffix(".db")).exists()
        }
    }

    suspend fun backupFileExists(context: Context): Boolean =
        withContext(Dispatchers.IO) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return@withContext File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ),
                    BACKUP_FILE_NAME
                ).exists()
            }

            val resolver = context.contentResolver
            val volumes = listOf(
                MediaStore.VOLUME_EXTERNAL_PRIMARY,
                MediaStore.VOLUME_EXTERNAL
            )

            volumes.forEach { volume ->
                val collection =
                    MediaStore.Downloads.getContentUri(volume)

                resolver.query(
                    collection,
                    arrayOf(MediaStore.Downloads._ID),
                    "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                    arrayOf(BACKUP_FILE_NAME),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return@withContext true
                    }
                }
            }

            false
        }


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

    /**
     * Read backup file from Downloads folder
     */
    suspend fun readBackupFromDownloads(context: Context): Result<InputStream> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Strategy 1: Query MediaStore by exact name
                val uri = findBackupFileUri(context)
                if (uri != null) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        return@withContext Result.success(inputStream)
                    }
                }
                
                // Strategy 2: Scan all downloads
                val uriFromScan = findBackupFileByScanning(context)
                if (uriFromScan != null) {
                    val inputStream = context.contentResolver.openInputStream(uriFromScan)
                    if (inputStream != null) {
                        return@withContext Result.success(inputStream)
                    }
                }
                
                Result.failure(Exception("Backup file does not exist in Downloads folder"))
            } else {
                // Android 9 and below - Read via File
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BACKUP_FILE_NAME)
                if (file.exists()) {
                    Result.success(FileInputStream(file))
                } else {
                    Result.failure(Exception("Backup file does not exist"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun findBackupFileUri(context: Context): Uri? {

        // Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val volumes = listOf(
                MediaStore.VOLUME_EXTERNAL_PRIMARY,
                MediaStore.VOLUME_EXTERNAL
            )

            volumes.forEach { volume ->
                val collection = MediaStore.Downloads.getContentUri(volume)

                resolver.query(
                    collection,
                    arrayOf(MediaStore.Downloads._ID),
                    "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                    arrayOf(BACKUP_FILE_NAME),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        return ContentUris.withAppendedId(collection, id)
                    }
                }
            }

//            return null
        }

        // Android 9 and below
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            BACKUP_FILE_NAME
        )

        return if (file.exists()) {
            Uri.fromFile(file)
        } else {
            null
        }
    }


    @SuppressLint("Range")
    private suspend fun findBackupFileUri2(context: Context): android.net.Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val volumes = listOf(MediaStore.VOLUME_EXTERNAL_PRIMARY, MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.RELATIVE_PATH)
            
            // Try both volumes
            for (volume in volumes) {
                val collection = MediaStore.Downloads.getContentUri(volume)
                
                // Strategy 1: Query by exact DISPLAY_NAME match
                val searchNames = listOf(BACKUP_FILE_NAME, BACKUP_FILE_NAME.removeSuffix(".db"))
                for (fileName in searchNames) {
                    val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
                    val selectionArgs = arrayOf(fileName)
                    
                    contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                            return@withContext android.net.Uri.withAppendedPath(collection, id.toString())
                        }
                    }
                }

                // Strategy 2: Query by DISPLAY_NAME using LIKE (case-insensitive, partial match)
                val likePattern = "%dolphin_db_backup%"
                val selectionLike = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
                val selectionArgsLike = arrayOf(likePattern)
                
                contentResolver.query(collection, projection, selectionLike, selectionArgsLike, "${MediaStore.Downloads.DATE_MODIFIED} DESC")?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME))
                        if (displayName.contains("dolphin_db_backup", ignoreCase = true)) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                            return@withContext android.net.Uri.withAppendedPath(collection, id.toString())
                        }
                    }
                }

                // Strategy 3: Query by RELATIVE_PATH and DISPLAY_NAME combination
                val selectionPath = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
                val selectionArgsPath = arrayOf("Download/%", "%dolphin_db_backup%")
                
                contentResolver.query(collection, projection, selectionPath, selectionArgsPath, "${MediaStore.Downloads.DATE_MODIFIED} DESC")?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME))
                        if (displayName.contains("dolphin_db_backup", ignoreCase = true)) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                            return@withContext android.net.Uri.withAppendedPath(collection, id.toString())
                        }
                    }
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("Range")
    private suspend fun findBackupFileByScanning(context: Context): android.net.Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val volumes = listOf(MediaStore.VOLUME_EXTERNAL_PRIMARY, MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.RELATIVE_PATH)
            
            // Try both volumes
            for (volume in volumes) {
                val collection = MediaStore.Downloads.getContentUri(volume)
                
                // Query all downloads and find by name (broader search)
                contentResolver.query(
                    collection,
                    projection,
                    null,
                    null,
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val searchNames = listOf(BACKUP_FILE_NAME, BACKUP_FILE_NAME.removeSuffix(".db"), "dolphin_db_backup", "dolphin_db_backup.db")
                    while (cursor.moveToNext()) {
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME))
                        if (searchNames.any { displayName.equals(it, ignoreCase = true) || displayName.contains(it, ignoreCase = true) }) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                            return@withContext android.net.Uri.withAppendedPath(collection, id.toString())
                        }
                    }
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveViaMediaStore(context: Context, sourceFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            // Step 1: Delete ALL existing backup files (including numbered versions like (1), (2), etc.)
//            deleteAllBackupFiles(contentResolver, collection)

            deleteExistingBackup(contentResolver, collection)

            // Step 2: Create new file entry with exact filename
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, BACKUP_FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, BACKUP_MIME_TYPE)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
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

    @SuppressLint("Range")
    private suspend fun deleteAllBackupFiles(contentResolver: android.content.ContentResolver, collection: android.net.Uri) = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
            
            // Query all files in Downloads that match backup file pattern (including numbered versions)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%dolphin_db_backup%")
            
            contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val urisToDelete = mutableListOf<android.net.Uri>()
                
                // Collect all URIs to delete
                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME))
                    // Delete if it matches any variation of backup filename (including numbered versions)
                    if (displayName.contains("dolphin_db_backup", ignoreCase = true)) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                        val uriToDelete = android.net.Uri.withAppendedPath(collection, id.toString())
                        urisToDelete.add(uriToDelete)
                    }
                }
                
                // Delete all collected URIs
                urisToDelete.forEach { uriToDelete ->
                    try {
                        contentResolver.delete(uriToDelete, null, null)
                    } catch (e: Exception) {
                        // Ignore individual delete errors
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors during deletion
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

