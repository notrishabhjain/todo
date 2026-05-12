package com.procrastinationkiller.presentation.dashboard

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var viewModel: DashboardViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTaskRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows loading`() = runTest {
        viewModel = DashboardViewModel(fakeRepository)
        // Before collecting, it should be in loading state initially
        assertEquals(true, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `pending task count reflects pending and in-progress tasks`() = runTest {
        fakeRepository.setTasks(
            listOf(
                createTask(1, "Task 1", status = "PENDING"),
                createTask(2, "Task 2", status = "IN_PROGRESS"),
                createTask(3, "Task 3", status = "COMPLETED")
            )
        )
        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.pendingTaskCount)
    }

    @Test
    fun `high priority count includes HIGH and CRITICAL tasks`() = runTest {
        fakeRepository.setTasks(
            listOf(
                createTask(1, "Task 1", priority = "HIGH", status = "PENDING"),
                createTask(2, "Task 2", priority = "CRITICAL", status = "PENDING"),
                createTask(3, "Task 3", priority = "LOW", status = "PENDING"),
                createTask(4, "Task 4", priority = "MEDIUM", status = "PENDING")
            )
        )
        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.highPriorityCount)
    }

    @Test
    fun `completion rate is calculated correctly`() = runTest {
        fakeRepository.setTasks(
            listOf(
                createTask(1, "Task 1", status = "COMPLETED"),
                createTask(2, "Task 2", status = "COMPLETED"),
                createTask(3, "Task 3", status = "PENDING"),
                createTask(4, "Task 4", status = "PENDING")
            )
        )
        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals(0.5f, viewModel.uiState.value.completionRate)
    }

    @Test
    fun `empty task list sets loading to false`() = runTest {
        fakeRepository.setTasks(emptyList())
        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, viewModel.uiState.value.pendingTaskCount)
        assertEquals(0f, viewModel.uiState.value.completionRate)
    }

    private fun createTask(
        id: Long,
        title: String,
        priority: String = "MEDIUM",
        status: String = "PENDING",
        deadline: Long? = null,
        completedAt: Long? = null
    ) = TaskEntity(
        id = id,
        title = title,
        priority = priority,
        status = status,
        deadline = deadline,
        completedAt = completedAt
    )

    private class FakeTaskRepository : TaskRepository {
        private val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
        private val tasks = mutableListOf<TaskEntity>()

        fun setTasks(taskList: List<TaskEntity>) {
            tasks.clear()
            tasks.addAll(taskList)
            tasksFlow.value = taskList
        }

        override fun getAllTasks(): Flow<List<TaskEntity>> = tasksFlow
        override fun getTasksByStatus(status: String): Flow<List<TaskEntity>> = tasksFlow
        override suspend fun getTaskById(id: Long): TaskEntity? = tasks.find { it.id == id }
        override suspend fun insertTask(task: TaskEntity): Long {
            tasks.add(task)
            tasksFlow.value = tasks.toList()
            return task.id
        }
        override suspend fun updateTask(task: TaskEntity) {
            val index = tasks.indexOfFirst { it.id == task.id }
            if (index >= 0) tasks[index] = task
            tasksFlow.value = tasks.toList()
        }
        override suspend fun deleteTask(task: TaskEntity) {
            tasks.removeAll { it.id == task.id }
            tasksFlow.value = tasks.toList()
        }
    }
}
