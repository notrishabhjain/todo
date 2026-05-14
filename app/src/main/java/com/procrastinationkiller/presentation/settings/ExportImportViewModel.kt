package com.procrastinationkiller.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.export.ExportFormat
import com.procrastinationkiller.data.export.ExportImportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportImportUiState(
    val exportMessage: String = "",
    val importMessage: String = "",
    val autoExportEnabled: Boolean = false,
    val pendingExportContent: String? = null,
    val pendingExportFormat: ExportFormat? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false
)

@HiltViewModel
class ExportImportViewModel @Inject constructor(
    private val exportImportService: ExportImportService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportImportUiState())
    val uiState: StateFlow<ExportImportUiState> = _uiState.asStateFlow()

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = exportImportService.exportTasks(ExportFormat.CSV)
            _uiState.update { it.copy(
                pendingExportContent = result.content,
                pendingExportFormat = ExportFormat.CSV,
                isExporting = false
            ) }
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = exportImportService.exportTasks(ExportFormat.JSON)
            _uiState.update { it.copy(
                pendingExportContent = result.content,
                pendingExportFormat = ExportFormat.JSON,
                isExporting = false
            ) }
        }
    }

    fun writeExportToUri(uri: Uri, context: Context) {
        val content = _uiState.value.pendingExportContent ?: return
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                }
                _uiState.update { it.copy(
                    exportMessage = "Tasks exported successfully",
                    pendingExportContent = null,
                    pendingExportFormat = null
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    exportMessage = "Export failed: ${e.message}",
                    pendingExportContent = null,
                    pendingExportFormat = null
                ) }
            }
        }
    }

    fun readImportFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                } ?: ""

                // Detect format from content
                val format = if (content.trimStart().startsWith("[") || content.trimStart().startsWith("{")) {
                    ExportFormat.JSON
                } else {
                    ExportFormat.CSV
                }

                val result = exportImportService.importTasks(content, format)
                val message = if (result.errors.isEmpty()) {
                    "Successfully imported ${result.importedCount} tasks"
                } else {
                    "Imported ${result.importedCount} tasks with ${result.errors.size} errors"
                }
                _uiState.update { it.copy(importMessage = message, isImporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(importMessage = "Import failed: ${e.message}", isImporting = false) }
            }
        }
    }

    fun clearPendingExport() {
        _uiState.update { it.copy(
            pendingExportContent = null,
            pendingExportFormat = null
        ) }
    }

    fun toggleAutoExport(enabled: Boolean) {
        _uiState.update { it.copy(autoExportEnabled = enabled) }
    }
}
