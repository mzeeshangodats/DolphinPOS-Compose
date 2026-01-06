package com.retail.dolphinpos.domain.repositories.backup

import java.io.InputStream
import java.io.OutputStream

interface DatabaseBackupRepository {
    suspend fun backupDatabase(outputStream: OutputStream): Result<Unit>
    suspend fun restoreDatabase(inputStream: InputStream): Result<Unit>
    suspend fun backupDatabaseToFile(): Result<Unit>
    suspend fun restoreDatabaseFromFile(): Result<Unit>
    fun getDatabasePath(): String
    fun getBackupFilePath(): String
    suspend fun closeDatabase()
    suspend fun reopenDatabase()
}

