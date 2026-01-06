package com.retail.dolphinpos.presentation.features.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.domain.usecases.backup.BackupDatabaseUseCase
import com.retail.dolphinpos.domain.usecases.backup.RestoreDatabaseUseCase
import com.retail.dolphinpos.domain.usecases.backup.BackupDatabaseToFileUseCase
import com.retail.dolphinpos.domain.usecases.backup.RestoreDatabaseFromFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

sealed class BackupUiEvent {
    object ShowLoading : BackupUiEvent()
    object HideLoading : BackupUiEvent()
    data class ShowError(val message: String) : BackupUiEvent()
    data class ShowSuccess(val message: String) : BackupUiEvent()
    object RestartApp : BackupUiEvent()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupDatabaseUseCase: BackupDatabaseUseCase,
    private val restoreDatabaseUseCase: RestoreDatabaseUseCase,
    private val backupDatabaseToFileUseCase: BackupDatabaseToFileUseCase,
    private val restoreDatabaseFromFileUseCase: RestoreDatabaseFromFileUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<BackupUiEvent>()
    val uiEvent: SharedFlow<BackupUiEvent> = _uiEvent.asSharedFlow()

    suspend fun backupDatabase(outputStream: OutputStream) {
        try {
            _isLoading.value = true
            _uiEvent.emit(BackupUiEvent.ShowLoading)

            val result = backupDatabaseUseCase(outputStream)
            result.fold(
                onSuccess = {
                    _uiEvent.emit(BackupUiEvent.ShowSuccess("Backup completed successfully"))
                },
                onFailure = { exception ->
                    _uiEvent.emit(BackupUiEvent.ShowError("Backup failed: ${exception.message}"))
                }
            )
        } catch (e: Exception) {
            _uiEvent.emit(BackupUiEvent.ShowError("Backup failed: ${e.message}"))
        } finally {
            _isLoading.value = false
            _uiEvent.emit(BackupUiEvent.HideLoading)
        }
    }

    suspend fun restoreDatabase(inputStream: InputStream) {
        try {
            _isLoading.value = true
            _uiEvent.emit(BackupUiEvent.ShowLoading)

            val result = restoreDatabaseUseCase(inputStream)
            result.fold(
                onSuccess = {
                    _uiEvent.emit(BackupUiEvent.ShowSuccess("Restore completed successfully. App will restart."))
                    _uiEvent.emit(BackupUiEvent.RestartApp)
                },
                onFailure = { exception ->
                    _uiEvent.emit(BackupUiEvent.ShowError("Restore failed: ${exception.message}"))
                }
            )
        } catch (e: Exception) {
            _uiEvent.emit(BackupUiEvent.ShowError("Restore failed: ${e.message}"))
        } finally {
            _isLoading.value = false
            _uiEvent.emit(BackupUiEvent.HideLoading)
        }
    }

    suspend fun backupDatabaseToFile() {
        try {
            _isLoading.value = true
            _uiEvent.emit(BackupUiEvent.ShowLoading)

            val result = backupDatabaseToFileUseCase()
            result.fold(
                onSuccess = {
                    _uiEvent.emit(BackupUiEvent.ShowSuccess("Backup completed successfully"))
                },
                onFailure = { exception ->
                    _uiEvent.emit(BackupUiEvent.ShowError("Backup failed: ${exception.message}"))
                }
            )
        } catch (e: Exception) {
            _uiEvent.emit(BackupUiEvent.ShowError("Backup failed: ${e.message}"))
        } finally {
            _isLoading.value = false
            _uiEvent.emit(BackupUiEvent.HideLoading)
        }
    }

    suspend fun restoreDatabaseFromFile() {
        try {
            _isLoading.value = true
            _uiEvent.emit(BackupUiEvent.ShowLoading)

            val result = restoreDatabaseFromFileUseCase()
            result.fold(
                onSuccess = {
                    _uiEvent.emit(BackupUiEvent.ShowSuccess("Restore completed successfully. App will restart."))
                    _uiEvent.emit(BackupUiEvent.RestartApp)
                },
                onFailure = { exception ->
                    _uiEvent.emit(BackupUiEvent.ShowError("Restore failed: ${exception.message}"))
                }
            )
        } catch (e: Exception) {
            _uiEvent.emit(BackupUiEvent.ShowError("Restore failed: ${e.message}"))
        } finally {
            _isLoading.value = false
            _uiEvent.emit(BackupUiEvent.HideLoading)
        }
    }
}

