package com.procrastinationkiller.data.export

import com.procrastinationkiller.data.local.entity.TaskEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor() {

    companion object {
        val HEADERS = listOf(
            "title", "description", "priority", "status",
            "reminder_mode", "deadline", "created_at", "completed_at"
        )
    }

    fun export(tasks: List<TaskEntity>): String {
        val sb = StringBuilder()

        // Write header
        sb.appendLine(HEADERS.joinToString(","))

        // Write rows
        for (task in tasks) {
            sb.appendLine(taskToRow(task))
        }

        return sb.toString()
    }

    private fun taskToRow(task: TaskEntity): String {
        return listOf(
            escapeCsv(task.title),
            escapeCsv(task.description),
            escapeCsv(task.priority),
            escapeCsv(task.status),
            escapeCsv(task.reminderMode),
            task.deadline?.toString() ?: "",
            task.createdAt.toString(),
            task.completedAt?.toString() ?: ""
        ).joinToString(",")
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
