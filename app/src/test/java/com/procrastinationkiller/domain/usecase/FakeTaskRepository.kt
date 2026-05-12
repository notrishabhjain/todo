package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTaskRepository : TaskRepository {

    private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllTasks(): Flow<List<TaskEntity>> = tasks

    override fun getTasksByStatus(status: String): Flow<List<TaskEntity>> =
        tasks.map { list -> list.filter { it.status == status } }

    override suspend fun getTaskById(id: Long): TaskEntity? =
        tasks.value.find { it.id == id }

    override suspend fun insertTask(task: TaskEntity): Long {
        val id = nextId++
        val newTask = task.copy(id = id)
        tasks.value = tasks.value + newTask
        return id
    }

    override suspend fun updateTask(task: TaskEntity) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun deleteTask(task: TaskEntity) {
        tasks.value = tasks.value.filter { it.id != task.id }
    }

    fun setTasks(taskList: List<TaskEntity>) {
        tasks.value = taskList
    }
}
