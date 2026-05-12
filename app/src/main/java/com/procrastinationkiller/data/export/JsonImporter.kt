package com.procrastinationkiller.data.export

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskStatus
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonImporter @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val validPriorities = TaskPriority.entries.map { it.name }.toSet()
    private val validStatuses = TaskStatus.entries.map { it.name }.toSet()

    fun import(content: String): List<Result<TaskEntity>> {
        return try {
            val dtos = json.decodeFromString<List<TaskExportDto>>(content)
            dtos.map { dto -> dtoToTask(dto) }
        } catch (e: Exception) {
            listOf(Result.failure(IllegalArgumentException("Invalid JSON format: ${e.message}")))
        }
    }

    private fun dtoToTask(dto: TaskExportDto): Result<TaskEntity> {
        return try {
            val rawPriority = dto.priority.uppercase().ifEmpty { "MEDIUM" }
            val rawStatus = dto.status.uppercase().ifEmpty { "PENDING" }

            val task = TaskEntity(
                title = dto.title,
                description = dto.description,
                priority = if (rawPriority in validPriorities) rawPriority else "MEDIUM",
                status = if (rawStatus in validStatuses) rawStatus else "PENDING",
                reminderMode = dto.reminderMode.ifEmpty { "NORMAL" },
                deadline = dto.deadline,
                createdAt = dto.createdAt,
                completedAt = dto.completedAt
            )
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
