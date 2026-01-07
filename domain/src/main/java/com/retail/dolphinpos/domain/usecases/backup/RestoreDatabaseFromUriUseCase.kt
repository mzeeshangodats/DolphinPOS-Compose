package com.retail.dolphinpos.domain.usecases.backup

import android.net.Uri
import com.retail.dolphinpos.domain.repositories.backup.DatabaseBackupRepository
import javax.inject.Inject

class RestoreDatabaseFromUriUseCase @Inject constructor(
    private val backupRepository: DatabaseBackupRepository
) {
    suspend operator fun invoke(uri: Uri): Result<Unit> {
        return try {
            backupRepository.closeDatabase()
            val result = backupRepository.restoreDatabaseFromUri(uri)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

