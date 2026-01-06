package com.retail.dolphinpos.domain.repositories.backup

import java.io.InputStream
import java.io.OutputStream

interface DatabaseBackupRepository {
    suspend fun backupDatabase(outputStream: OutputStream): Result<Unit>
    suspend fun restoreDatabase(inputStream: InputStream): Result<Unit>
    fun getDatabasePath(): String
    suspend fun closeDatabase()
    suspend fun reopenDatabase()
}

