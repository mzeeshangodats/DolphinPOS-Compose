package com.retail.dolphinpos.data.datasource

import android.content.Context
import com.retail.dolphinpos.data.room.DolphinDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseBackupDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: DolphinDatabase
) : DatabaseBackupDataSource {

    companion object {
        private const val DATABASE_NAME = "dolphin_retail_pos"
        private const val BACKUP_FILE_NAME = "dolphin_db_backup"
        private const val BUFFER_SIZE = 8192
    }

    override fun getDatabasePath(): String {
        return context.getDatabasePath(DATABASE_NAME).absolutePath
    }

    override fun getBackupFilePath(): String {
        return ExternalStorageHelper.getBackupFilePath(context)
    }

    override suspend fun closeDatabase() = withContext(Dispatchers.IO) {
        database.close()
    }

    override suspend fun reopenDatabase() = withContext(Dispatchers.IO) {
        // Database will be reopened automatically on next access via Room
        // This is a no-op as Room handles connection pooling
    }



    override suspend fun backupDatabaseToFile(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file does not exist"))
            }

            // Checkpoint WAL to ensure consistent backup
            try {
                val db = database.openHelper.writableDatabase
                db.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                db.close()
            } catch (e: Exception) {
                // If checkpoint fails, continue anyway
            }

            // Save to Downloads folder (persists after uninstall)
            ExternalStorageHelper.saveBackupToDownloads(context, dbFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreDatabaseFromFile(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Read backup from Downloads folder
            val backupInputStreamResult = ExternalStorageHelper.readBackupFromDownloads(context)
            val backupInputStream = backupInputStreamResult.getOrElse {
                return@withContext Result.failure(it)
            }

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val dbDir = dbFile.parentFile

            if (!dbDir.exists()) {
                dbDir.mkdirs()
            }

            // Write to temporary file first
            val tempFile = File(dbDir, "${DATABASE_NAME}.tmp")

            backupInputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            // Replace original database file
            if (dbFile.exists()) {
                dbFile.delete()
            }
            tempFile.renameTo(dbFile)

            // Delete WAL and SHM files if they exist
            File(dbDir, "${DATABASE_NAME}-wal").delete()
            File(dbDir, "${DATABASE_NAME}-shm").delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

