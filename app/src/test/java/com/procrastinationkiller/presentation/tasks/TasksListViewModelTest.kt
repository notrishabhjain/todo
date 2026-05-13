package com.procrastinationkiller.presentation.tasks

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.repository.TaskRepository
import com.procrastinationkiller.domain.usecase.GetTasksUseCase
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var updateTaskUseCase: com.procrastinationkiller.domain.usecase.UpdateTaskUseCase
    private lateinit var viewModel: TasksListViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTaskRepository()
        getTasksUseCase = GetTasksUseCase(fakeRepository)
        updateTaskUseCase = com.procrastinationkiller.domain.usecase.UpdateTaskUseCase(fakeRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads tasks`() = runTest {
        fakeRepository.setTasks(
            listOf(
                createTask(1, "Task 1"),
                createTask(2, "Task 2")
            )
        )
        viewModel = TasksListViewModel(getTasksUseCase, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.tasks.size)
    }

    @Test
    fun `setFilter filters tasks by priority`() = runTest {
        fakeRepository.setTasks(
            listOf(
                createTask(1, "High task", priority = "HIGH"),
                createTask(2, "Low task", priority = "LOW"),
                createTask(3, "High task 2", priority = "HIGH")
            )
        )
        viewModel = TasksListViewModel(getTasksUseCase, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        viewModel.setFilter(TaskPriority.HIGH)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.tasks.size)
        assertTrue(viewModel.uiState.value.tasks.all { it.priority == "HIGH" })
    }

    @Test
    fun `toggling same filter deselects it`() = runTest {
        fakeRepository.setTasks(
            listOf(
                createTask(1, "Task 1", priority = "HIGH"),
                createTask(2, "Task 2", priority = "LOW")
            )
        )
        viewModel = TasksListViewModel(getTasksUseCase, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        viewModel.setFilter(TaskPriority.HIGH)
        advanceUntilIdle()
        assertEquals(TaskPriority.HIGH, viewModel.uiState.value.selectedPriority)

        viewModel.setFilter(TaskPriority.HIGH)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedPriority)
        assertEquals(2, viewModel.uiState.value.tasks.size)
    }

    @Test
    fun `showCreateDialog toggles dialog state`() = runTest {
        viewModel = TasksListViewModel(getTasksUseCase, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showCreateDialog)
        viewModel.showCreateDialog()
        assertTrue(viewModel.uiState.value.showCreateDialog)
        viewModel.hideCreateDialog()
        assertFalse(viewModel.uiState.value.showCreateDialog)
    }

    @Test
    fun `createTask adds task to repository`() = runTest {
        viewModel = TasksListViewModel(getTasksUseCase, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        viewModel.createTask("New Task", "Description", TaskPriority.HIGH, null)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.tasks.size)
        assertEquals("New Task", viewModel.uiState.value.tasks[0].title)
        assertEquals("HIGH", viewModel.uiState.value.tasks[0].priority)
    }

    @Test
    fun `completeTask marks task as completed`() = runTest {
        fakeRepository.setTasks(
            listOf(
                createTask(1, "Task 1", status = "PENDING"),
                createTask(2, "Task 2", status = "PENDING")
            )
        )
        val updateTaskUseCase = com.procrastinationkiller.domain.usecase.UpdateTaskUseCase(fakeRepository)
        viewModel = TasksListViewModel(getTasksUseCase, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        viewModel.completeTask(1)
        advanceUntilIdle()

        val updatedTask = fakeRepository.getTaskById(1)
        assertEquals("COMPLETED", updatedTask?.status)
        assertTrue(updatedTask?.completedAt != null)
    }

    private fun createTask(
        id: Long,
        title: String,
        priority: String = "MEDIUM",
        status: String = "PENDING"
    ) = TaskEntity(
        id = id,
        title = title,
        priority = priority,
        status = status
    )

    private class FakeTaskRepository : TaskRepository {
        private val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
        private val tasks = mutableListOf<TaskEntity>()
        private var nextId = 100L

        fun setTasks(taskList: List<TaskEntity>) {
            tasks.clear()
            tasks.addAll(taskList)
            tasksFlow.value = taskList
        }

        override fun getAllTasks(): Flow<List<TaskEntity>> = tasksFlow
        override fun getTasksByStatus(status: String): Flow<List<TaskEntity>> = tasksFlow
        override suspend fun getTaskById(id: Long): TaskEntity? = tasks.find { it.id == id }
        override suspend fun insertTask(task: TaskEntity): Long {
            val id = nextId++
            tasks.add(task.copy(id = id))
            tasksFlow.value = tasks.toList()
            return id
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
