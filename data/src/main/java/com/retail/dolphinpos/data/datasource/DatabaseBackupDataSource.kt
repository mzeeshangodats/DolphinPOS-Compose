package com.retail.dolphinpos.data.datasource

import android.net.Uri

interface DatabaseBackupDataSource {
    suspend fun backupDatabaseToFile(): Result<Unit>
    suspend fun restoreDatabaseFromUri(uri: Uri): Result<Unit>
    fun getDatabasePath(): String
    suspend fun closeDatabase()
    suspend fun reopenDatabase()
}

