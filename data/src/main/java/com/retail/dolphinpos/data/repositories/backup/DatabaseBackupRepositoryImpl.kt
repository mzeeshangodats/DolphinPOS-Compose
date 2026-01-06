package com.retail.dolphinpos.data.repositories.backup

import com.retail.dolphinpos.data.datasource.DatabaseBackupDataSource
import com.retail.dolphinpos.domain.repositories.backup.DatabaseBackupRepository
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class DatabaseBackupRepositoryImpl @Inject constructor(
    private val backupDataSource: DatabaseBackupDataSource
) : DatabaseBackupRepository {
    override suspend fun backupDatabase(outputStream: OutputStream): Result<Unit> {
        return backupDataSource.backupDatabase(outputStream)
    }

    override suspend fun restoreDatabase(inputStream: InputStream): Result<Unit> {
        return backupDataSource.restoreDatabase(inputStream)
    }

    override fun getDatabasePath(): String {
        return backupDataSource.getDatabasePath()
    }

    override suspend fun closeDatabase() {
        backupDataSource.closeDatabase()
    }

    override suspend fun reopenDatabase() {
        backupDataSource.reopenDatabase()
    }
}

