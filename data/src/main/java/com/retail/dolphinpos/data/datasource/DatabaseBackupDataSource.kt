package com.retail.dolphinpos.data.datasource

import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

interface DatabaseBackupDataSource {
    suspend fun backupDatabaseToFile(): Result<Unit>
    suspend fun restoreDatabaseFromFile(): Result<Unit>
    suspend fun restoreDatabaseFromUri(uri: Uri): Result<Unit>
    fun getDatabasePath(): String
    fun getBackupFilePath(): String
    suspend fun closeDatabase()
    suspend fun reopenDatabase()
}

