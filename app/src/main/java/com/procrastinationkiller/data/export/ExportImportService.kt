package com.procrastinationkiller.data.export

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportFormat {
    CSV,
    JSON
}

data class ExportResult(
    val content: String,
    val format: ExportFormat,
    val taskCount: Int
)

data class ImportResult(
    val importedCount: Int,
    val errors: List<String>
)

@Singleton
class ExportImportService @Inject constructor(
    private val taskRepository: TaskRepository,
    private val csvExporter: CsvExporter,
    private val jsonExporter: JsonExporter,
    private val csvImporter: CsvImporter,
    private val jsonImporter: JsonImporter
) {

    suspend fun exportTasks(format: ExportFormat): ExportResult {
        val tasks = taskRepository.getAllTasks().first()
        val content = when (format) {
            ExportFormat.CSV -> csvExporter.export(tasks)
            ExportFormat.JSON -> jsonExporter.export(tasks)
        }
        return ExportResult(content, format, tasks.size)
    }

    suspend fun importTasks(content: String, format: ExportFormat): ImportResult {
        val result = when (format) {
            ExportFormat.CSV -> csvImporter.import(content)
            ExportFormat.JSON -> jsonImporter.import(content)
        }

        val errors = mutableListOf<String>()
        var importedCount = 0

        for ((index, taskResult) in result.withIndex()) {
            taskResult.fold(
                onSuccess = { task ->
                    taskRepository.insertTask(task)
                    importedCount++
                },
                onFailure = { error ->
                    errors.add("Row ${index + 1}: ${error.message}")
                }
            )
        }

        return ImportResult(importedCount, errors)
    }
}
