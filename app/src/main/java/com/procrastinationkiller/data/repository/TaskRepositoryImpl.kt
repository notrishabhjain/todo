package com.procrastinationkiller.data.repository

import com.procrastinationkiller.data.local.dao.TaskDao
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    override fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
    override fun getTasksByStatus(status: String): Flow<List<TaskEntity>> = taskDao.getTasksByStatus(status)
    override suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)
    override suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    override suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    override suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
}
