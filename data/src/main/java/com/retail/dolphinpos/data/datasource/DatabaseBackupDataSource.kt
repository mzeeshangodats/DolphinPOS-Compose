package com.retail.dolphinpos.data.datasource

import java.io.InputStream
import java.io.OutputStream

interface DatabaseBackupDataSource {
    suspend fun backupDatabase(outputStream: OutputStream): Result<Unit>
    suspend fun restoreDatabase(inputStream: InputStream): Result<Unit>
    fun getDatabasePath(): String
    suspend fun closeDatabase()
    suspend fun reopenDatabase()
}

