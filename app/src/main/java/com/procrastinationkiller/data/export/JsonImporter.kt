package com.procrastinationkiller.data.export

import com.procrastinationkiller.data.local.entity.TaskEntity
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonImporter @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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
            val task = TaskEntity(
                title = dto.title,
                description = dto.description,
                priority = dto.priority.uppercase().ifEmpty { "MEDIUM" },
                status = dto.status.uppercase().ifEmpty { "PENDING" },
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
