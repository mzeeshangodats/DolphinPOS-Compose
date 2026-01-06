package com.retail.dolphinpos.domain.usecases.backup

import com.retail.dolphinpos.domain.repositories.backup.DatabaseBackupRepository
import java.io.OutputStream
import javax.inject.Inject

class BackupDatabaseUseCase @Inject constructor(
    private val backupRepository: DatabaseBackupRepository
) {
    suspend operator fun invoke(outputStream: OutputStream): Result<Unit> {
        return try {
            // Backup handles WAL checkpoint internally before copying
            val result = backupRepository.backupDatabase(outputStream)
            // Database will be reopened automatically on next access
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

