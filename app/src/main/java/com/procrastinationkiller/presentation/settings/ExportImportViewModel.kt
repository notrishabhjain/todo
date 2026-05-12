package com.procrastinationkiller.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.export.ExportFormat
import com.procrastinationkiller.data.export.ExportImportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportImportUiState(
    val exportMessage: String = "",
    val importMessage: String = "",
    val autoExportEnabled: Boolean = false,
    val lastExportContent: String = ""
)

@HiltViewModel
class ExportImportViewModel @Inject constructor(
    private val exportImportService: ExportImportService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportImportUiState())
    val uiState: StateFlow<ExportImportUiState> = _uiState.asStateFlow()

    fun exportCsv() {
        viewModelScope.launch {
            val result = exportImportService.exportTasks(ExportFormat.CSV)
            _uiState.value = _uiState.value.copy(
                exportMessage = "Exported ${result.taskCount} tasks as CSV",
                lastExportContent = result.content
            )
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            val result = exportImportService.exportTasks(ExportFormat.JSON)
            _uiState.value = _uiState.value.copy(
                exportMessage = "Exported ${result.taskCount} tasks as JSON",
                lastExportContent = result.content
            )
        }
    }

    fun triggerImport() {
        _uiState.value = _uiState.value.copy(
            importMessage = "File picker would open here"
        )
    }

    fun importContent(content: String, format: ExportFormat) {
        viewModelScope.launch {
            val result = exportImportService.importTasks(content, format)
            val message = if (result.errors.isEmpty()) {
                "Successfully imported ${result.importedCount} tasks"
            } else {
                "Imported ${result.importedCount} tasks with ${result.errors.size} errors"
            }
            _uiState.value = _uiState.value.copy(importMessage = message)
        }
    }

    fun toggleAutoExport(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoExportEnabled = enabled)
    }
}
