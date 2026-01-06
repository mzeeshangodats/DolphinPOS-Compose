package com.retail.dolphinpos.domain.usecases.backup

import com.retail.dolphinpos.domain.repositories.backup.DatabaseBackupRepository
import java.io.InputStream
import javax.inject.Inject

class RestoreDatabaseUseCase @Inject constructor(
    private val backupRepository: DatabaseBackupRepository
) {
    suspend operator fun invoke(inputStream: InputStream): Result<Unit> {
        return try {
            // Close database before restore
            backupRepository.closeDatabase()
            val result = backupRepository.restoreDatabase(inputStream)
            // Database will be reopened automatically on next access after app restart
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

