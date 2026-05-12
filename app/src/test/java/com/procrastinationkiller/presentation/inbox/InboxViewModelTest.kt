package com.procrastinationkiller.presentation.inbox

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskSuggestion
import com.procrastinationkiller.domain.repository.TaskRepository
import com.procrastinationkiller.domain.usecase.ApproveTaskUseCase
import com.procrastinationkiller.domain.usecase.RejectTaskUseCase
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var approveUseCase: ApproveTaskUseCase
    private lateinit var rejectUseCase: RejectTaskUseCase
    private lateinit var viewModel: InboxViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTaskRepository()
        approveUseCase = ApproveTaskUseCase(fakeRepository)
        rejectUseCase = RejectTaskUseCase(FakeLearningDataDao())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not loading with empty suggestions`() = runTest {
        viewModel = InboxViewModel(approveUseCase, rejectUseCase)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.suggestions.isEmpty())
    }

    @Test
    fun `addSuggestion adds to suggestions list`() = runTest {
        viewModel = InboxViewModel(approveUseCase, rejectUseCase)
        advanceUntilIdle()

        val suggestion = createSuggestion("Test task", "whatsapp", "John")
        viewModel.addSuggestion(suggestion)

        assertEquals(1, viewModel.uiState.value.suggestions.size)
        assertEquals("Test task", viewModel.uiState.value.suggestions[0].suggestedTitle)
    }

    @Test
    fun `approveSuggestion removes from list and creates task`() = runTest {
        viewModel = InboxViewModel(approveUseCase, rejectUseCase)
        advanceUntilIdle()

        val suggestion = createSuggestion("Approve this", "telegram", "Alice")
        viewModel.addSuggestion(suggestion)
        assertEquals(1, viewModel.uiState.value.suggestions.size)

        viewModel.approveSuggestion(suggestion)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.suggestions.isEmpty())
        assertEquals("Task approved: Approve this", viewModel.uiState.value.message)
    }

    @Test
    fun `rejectSuggestion removes from list`() = runTest {
        viewModel = InboxViewModel(approveUseCase, rejectUseCase)
        advanceUntilIdle()

        val suggestion = createSuggestion("Reject this", "slack", "Bob")
        viewModel.addSuggestion(suggestion)

        viewModel.rejectSuggestion(suggestion)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.suggestions.isEmpty())
        assertEquals("Suggestion rejected", viewModel.uiState.value.message)
    }

    @Test
    fun `editSuggestion approves with modified values`() = runTest {
        viewModel = InboxViewModel(approveUseCase, rejectUseCase)
        advanceUntilIdle()

        val suggestion = createSuggestion("Original title", "gmail", "Carol")
        viewModel.addSuggestion(suggestion)

        viewModel.editSuggestion(suggestion, "Edited title", TaskPriority.HIGH)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.suggestions.isEmpty())
        assertEquals("Task saved: Edited title", viewModel.uiState.value.message)
    }

    @Test
    fun `clearMessage sets message to null`() = runTest {
        viewModel = InboxViewModel(approveUseCase, rejectUseCase)
        advanceUntilIdle()

        val suggestion = createSuggestion("Test", "app", "User")
        viewModel.addSuggestion(suggestion)
        viewModel.rejectSuggestion(suggestion)
        advanceUntilIdle()

        viewModel.clearMessage()
        assertEquals(null, viewModel.uiState.value.message)
    }

    private fun createSuggestion(
        title: String,
        sourceApp: String,
        sender: String
    ) = TaskSuggestion(
        suggestedTitle = title,
        description = "Test description",
        priority = TaskPriority.MEDIUM,
        dueDate = null,
        sourceApp = sourceApp,
        sender = sender,
        originalText = "Original notification text",
        confidence = 0.8f
    )

    private class FakeTaskRepository : TaskRepository {
        private val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
        private val tasks = mutableListOf<TaskEntity>()
        private var nextId = 1L

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

    private class FakeLearningDataDao : LearningDataDao {
        private val data = mutableListOf<LearningDataEntity>()
        private var nextId = 1L

        override fun getAllLearningData(): Flow<List<LearningDataEntity>> =
            MutableStateFlow(data.toList())

        override fun getLearningDataByLabel(label: String): Flow<List<LearningDataEntity>> =
            MutableStateFlow(data.filter { it.label == label })

        override suspend fun insertLearningData(data: LearningDataEntity): Long {
            val id = nextId++
            this.data.add(data.copy(id = id))
            return id
        }

        override suspend fun deleteOldData(before: Long) {
            data.removeAll { it.timestamp < before }
        }
    }
}
