package com.procrastinationkiller.data.export

import com.procrastinationkiller.data.local.entity.TaskEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvImporter @Inject constructor() {

    fun import(content: String): List<Result<TaskEntity>> {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        // Skip header line
        val dataLines = if (lines.first().lowercase().contains("title")) {
            lines.drop(1)
        } else {
            lines
        }

        return dataLines.map { line ->
            parseLine(line)
        }
    }

    private fun parseLine(line: String): Result<TaskEntity> {
        return try {
            val fields = parseCsvLine(line)
            if (fields.size < 3) {
                return Result.failure(IllegalArgumentException("Insufficient fields: expected at least 3, got ${fields.size}"))
            }

            val task = TaskEntity(
                title = fields[0],
                description = fields.getOrElse(1) { "" },
                priority = fields.getOrElse(2) { "MEDIUM" }.uppercase().ifEmpty { "MEDIUM" },
                status = fields.getOrElse(3) { "PENDING" }.uppercase().ifEmpty { "PENDING" },
                reminderMode = fields.getOrElse(4) { "NORMAL" }.ifEmpty { "NORMAL" },
                deadline = fields.getOrElse(5) { "" }.toLongOrNull(),
                createdAt = fields.getOrElse(6) { "" }.toLongOrNull() ?: System.currentTimeMillis(),
                completedAt = fields.getOrElse(7) { "" }.toLongOrNull()
            )
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && !inQuotes -> {
                    inQuotes = true
                }
                ch == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(ch)
                }
            }
            i++
        }
        fields.add(current.toString())

        return fields
    }
}
