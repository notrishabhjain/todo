package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskStatus
import com.procrastinationkiller.domain.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    suspend fun updateStatus(taskId: Long, newStatus: TaskStatus) {
        val task = taskRepository.getTaskById(taskId) ?: return
        val updatedTask = task.copy(
            status = newStatus.name,
            completedAt = if (newStatus == TaskStatus.COMPLETED) System.currentTimeMillis() else task.completedAt
        )
        taskRepository.updateTask(updatedTask)
    }

    suspend fun updatePriority(taskId: Long, newPriority: TaskPriority) {
        val task = taskRepository.getTaskById(taskId) ?: return
        taskRepository.updateTask(task.copy(priority = newPriority.name))
    }

    suspend fun updateTask(taskEntity: TaskEntity) {
        taskRepository.updateTask(taskEntity)
    }

    suspend fun deleteTask(taskId: Long) {
        val task = taskRepository.getTaskById(taskId) ?: return
        taskRepository.deleteTask(task)
    }
}
