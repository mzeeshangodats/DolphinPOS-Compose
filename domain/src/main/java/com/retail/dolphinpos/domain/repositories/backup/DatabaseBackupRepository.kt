package com.retail.dolphinpos.domain.repositories.backup

import android.net.Uri

interface DatabaseBackupRepository {
    suspend fun backupDatabaseToFile(): Result<Unit>
    suspend fun restoreDatabaseFromUri(uri: Uri): Result<Unit>
    fun getDatabasePath(): String
    suspend fun closeDatabase()
    suspend fun reopenDatabase()
}

