package com.procrastinationkiller.presentation.taskdetail

import androidx.lifecycle.SavedStateHandle
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.repository.TaskRepository
import com.procrastinationkiller.domain.usecase.UpdateTaskUseCase
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
class TaskDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var updateTaskUseCase: UpdateTaskUseCase
    private lateinit var viewModel: TaskDetailViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTaskRepository()
        updateTaskUseCase = UpdateTaskUseCase(fakeRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads task by id from savedStateHandle`() = runTest {
        val task = TaskEntity(id = 1, title = "Test Task", priority = "HIGH", status = "PENDING")
        fakeRepository.setTasks(listOf(task))

        val savedState = SavedStateHandle(mapOf("taskId" to 1L))
        viewModel = TaskDetailViewModel(savedState, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Test Task", viewModel.uiState.value.task?.title)
        assertEquals(TaskPriority.HIGH, viewModel.uiState.value.editPriority)
    }

    @Test
    fun `non-existent task sets task to null`() = runTest {
        val savedState = SavedStateHandle(mapOf("taskId" to 999L))
        viewModel = TaskDetailViewModel(savedState, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.task)
    }

    @Test
    fun `startEditing sets isEditing to true`() = runTest {
        val task = TaskEntity(id = 1, title = "Task", priority = "MEDIUM", status = "PENDING")
        fakeRepository.setTasks(listOf(task))

        val savedState = SavedStateHandle(mapOf("taskId" to 1L))
        viewModel = TaskDetailViewModel(savedState, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        viewModel.startEditing()
        assertTrue(viewModel.uiState.value.isEditing)
    }

    @Test
    fun `cancelEditing restores original values`() = runTest {
        val task = TaskEntity(id = 1, title = "Original", description = "Desc", priority = "MEDIUM", status = "PENDING")
        fakeRepository.setTasks(listOf(task))

        val savedState = SavedStateHandle(mapOf("taskId" to 1L))
        viewModel = TaskDetailViewModel(savedState, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        viewModel.startEditing()
        viewModel.updateTitle("Changed")
        viewModel.cancelEditing()

        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals("Original", viewModel.uiState.value.editTitle)
    }

    @Test
    fun `saveChanges updates task in repository`() = runTest {
        val task = TaskEntity(id = 1, title = "Old Title", priority = "MEDIUM", status = "PENDING")
        fakeRepository.setTasks(listOf(task))

        val savedState = SavedStateHandle(mapOf("taskId" to 1L))
        viewModel = TaskDetailViewModel(savedState, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        viewModel.startEditing()
        viewModel.updateTitle("New Title")
        viewModel.updatePriority(TaskPriority.HIGH)
        viewModel.saveChanges()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals("New Title", viewModel.uiState.value.task?.title)
        assertEquals("HIGH", viewModel.uiState.value.task?.priority)
    }

    @Test
    fun `completeTask changes status to COMPLETED`() = runTest {
        val task = TaskEntity(id = 1, title = "Task", priority = "MEDIUM", status = "PENDING")
        fakeRepository.setTasks(listOf(task))

        val savedState = SavedStateHandle(mapOf("taskId" to 1L))
        viewModel = TaskDetailViewModel(savedState, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        viewModel.completeTask()
        advanceUntilIdle()

        assertEquals("COMPLETED", viewModel.uiState.value.task?.status)
    }

    @Test
    fun `deleteTask sets task to null`() = runTest {
        val task = TaskEntity(id = 1, title = "Task", priority = "MEDIUM", status = "PENDING")
        fakeRepository.setTasks(listOf(task))

        val savedState = SavedStateHandle(mapOf("taskId" to 1L))
        viewModel = TaskDetailViewModel(savedState, fakeRepository, updateTaskUseCase)
        advanceUntilIdle()

        viewModel.deleteTask()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.task)
        assertEquals("Task deleted", viewModel.uiState.value.message)
    }

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
