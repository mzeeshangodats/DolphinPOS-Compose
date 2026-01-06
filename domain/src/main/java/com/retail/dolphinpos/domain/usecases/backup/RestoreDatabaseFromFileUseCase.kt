package com.retail.dolphinpos.domain.usecases.backup

import com.retail.dolphinpos.domain.repositories.backup.DatabaseBackupRepository
import javax.inject.Inject

class RestoreDatabaseFromFileUseCase @Inject constructor(
    private val backupRepository: DatabaseBackupRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            backupRepository.closeDatabase()
            val result = backupRepository.restoreDatabaseFromFile()
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

