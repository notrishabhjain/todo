package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskSuggestion
import com.procrastinationkiller.domain.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApproveTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    suspend operator fun invoke(suggestion: TaskSuggestion): Long {
        val taskEntity = TaskEntity(
            title = suggestion.suggestedTitle,
            description = suggestion.description,
            priority = suggestion.priority.name,
            status = "PENDING",
            deadline = suggestion.dueDate,
            createdAt = System.currentTimeMillis()
        )
        return taskRepository.insertTask(taskEntity)
    }
}
