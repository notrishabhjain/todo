package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskStatus
import com.procrastinationkiller.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class TaskFilter(
    val priority: TaskPriority? = null,
    val status: TaskStatus? = null,
    val sourceApp: String? = null,
    val sender: String? = null,
    val hasDueDate: Boolean? = null
)

enum class TaskSortOrder {
    CREATED_DESC,
    CREATED_ASC,
    DEADLINE_ASC,
    PRIORITY_DESC
}

@Singleton
class GetTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    operator fun invoke(
        filter: TaskFilter = TaskFilter(),
        sortOrder: TaskSortOrder = TaskSortOrder.CREATED_DESC
    ): Flow<List<TaskEntity>> {
        return taskRepository.getAllTasks().map { tasks ->
            var filtered = tasks

            // Filter out ARCHIVED, DELETED, and COMPLETED tasks unless explicitly requested
            if (filter.status == null) {
                filtered = filtered.filter {
                    it.status != TaskStatus.ARCHIVED.name &&
                        it.status != TaskStatus.DELETED.name &&
                        it.status != TaskStatus.COMPLETED.name
                }
            }

            filter.priority?.let { priority ->
                filtered = filtered.filter { it.priority == priority.name }
            }

            filter.status?.let { status ->
                filtered = filtered.filter { it.status == status.name }
            }

            filter.hasDueDate?.let { hasDue ->
                filtered = if (hasDue) {
                    filtered.filter { it.deadline != null }
                } else {
                    filtered.filter { it.deadline == null }
                }
            }

            when (sortOrder) {
                TaskSortOrder.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
                TaskSortOrder.CREATED_ASC -> filtered.sortedBy { it.createdAt }
                TaskSortOrder.DEADLINE_ASC -> filtered.sortedBy { it.deadline ?: Long.MAX_VALUE }
                TaskSortOrder.PRIORITY_DESC -> filtered.sortedByDescending { priorityWeight(it.priority) }
            }
        }
    }

    private fun priorityWeight(priority: String): Int {
        return when (priority) {
            "CRITICAL" -> 4
            "HIGH" -> 3
            "MEDIUM" -> 2
            "LOW" -> 1
            else -> 0
        }
    }
}
