package com.retail.dolphinpos.domain.usecases.backup

import com.retail.dolphinpos.domain.repositories.backup.DatabaseBackupRepository
import javax.inject.Inject

class BackupDatabaseToFileUseCase @Inject constructor(
    private val backupRepository: DatabaseBackupRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            backupRepository.backupDatabaseToFile()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

