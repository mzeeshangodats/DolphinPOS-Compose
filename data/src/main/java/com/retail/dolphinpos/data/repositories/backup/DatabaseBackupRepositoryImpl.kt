package com.retail.dolphinpos.data.repositories.backup

import android.net.Uri
import com.retail.dolphinpos.data.datasource.DatabaseBackupDataSource
import com.retail.dolphinpos.domain.repositories.backup.DatabaseBackupRepository
import javax.inject.Inject

class DatabaseBackupRepositoryImpl @Inject constructor(
    private val backupDataSource: DatabaseBackupDataSource
) : DatabaseBackupRepository {

    override fun getDatabasePath(): String {
        return backupDataSource.getDatabasePath()
    }

    override suspend fun closeDatabase() {
        backupDataSource.closeDatabase()
    }

    override suspend fun reopenDatabase() {
        backupDataSource.reopenDatabase()
    }

    override suspend fun backupDatabaseToFile(): Result<Unit> {
        return backupDataSource.backupDatabaseToFile()
    }

    override suspend fun restoreDatabaseFromUri(uri: Uri): Result<Unit> {
        return backupDataSource.restoreDatabaseFromUri(uri)
    }

}

