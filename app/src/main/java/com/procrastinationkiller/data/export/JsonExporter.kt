package com.procrastinationkiller.data.export

import com.procrastinationkiller.data.local.entity.TaskEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TaskExportDto(
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val reminderMode: String,
    val deadline: Long? = null,
    val createdAt: Long,
    val completedAt: Long? = null
)

@Singleton
class JsonExporter @Inject constructor() {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun export(tasks: List<TaskEntity>): String {
        val dtos = tasks.map { it.toDto() }
        return json.encodeToString(dtos)
    }

    private fun TaskEntity.toDto(): TaskExportDto {
        return TaskExportDto(
            title = title,
            description = description,
            priority = priority,
            status = status,
            reminderMode = reminderMode,
            deadline = deadline,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }
}
