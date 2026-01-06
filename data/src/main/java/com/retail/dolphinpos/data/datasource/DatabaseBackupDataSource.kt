package com.retail.dolphinpos.data.datasource

import java.io.InputStream
import java.io.OutputStream

interface DatabaseBackupDataSource {
    suspend fun backupDatabaseToFile(): Result<Unit>
    suspend fun restoreDatabaseFromFile(): Result<Unit>
    fun getDatabasePath(): String
    fun getBackupFilePath(): String
    suspend fun closeDatabase()
    suspend fun reopenDatabase()
}

