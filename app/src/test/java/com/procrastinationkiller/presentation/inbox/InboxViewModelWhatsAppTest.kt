package com.procrastinationkiller.presentation.inbox

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.dao.TaskSuggestionDao
import com.procrastinationkiller.data.local.entity.ContactEntity
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity
import com.procrastinationkiller.domain.engine.whatsapp.ChatType
import com.procrastinationkiller.domain.engine.whatsapp.WhatsAppContext
import com.procrastinationkiller.domain.model.ContactPriority
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskSuggestion
import com.procrastinationkiller.domain.repository.ContactRepository
import com.procrastinationkiller.domain.repository.TaskRepository
import com.procrastinationkiller.domain.usecase.ApproveTaskUseCase
import com.procrastinationkiller.domain.usecase.RejectTaskUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
class InboxViewModelWhatsAppTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTaskRepository: FakeTaskRepositoryForWhatsApp
    private lateinit var approveUseCase: ApproveTaskUseCase
    private lateinit var rejectUseCase: RejectTaskUseCase
    private lateinit var fakeSuggestionDao: FakeTaskSuggestionDaoForWhatsApp
    private lateinit var fakeContactRepository: FakeContactRepositoryForViewModel
    private lateinit var viewModel: InboxViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTaskRepository = FakeTaskRepositoryForWhatsApp()
        approveUseCase = ApproveTaskUseCase(fakeTaskRepository)
        rejectUseCase = RejectTaskUseCase(FakeLearningDataDaoForWhatsApp())
        fakeSuggestionDao = FakeTaskSuggestionDaoForWhatsApp()
        fakeContactRepository = FakeContactRepositoryForViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setContactAutoApprove updates contact auto-approve state`() = runTest {
        fakeContactRepository.addContact(
            ContactEntity(id = 1, name = "John", priority = "NORMAL", autoApprove = false)
        )
        viewModel = InboxViewModel(approveUseCase, rejectUseCase, fakeSuggestionDao, fakeContactRepository)
        advanceUntilIdle()

        val suggestion = createWhatsAppSuggestion("Task from John", "John")
        viewModel.addSuggestion(suggestion)

        viewModel.setContactAutoApprove(suggestion, true)
        advanceUntilIdle()

        val contact = fakeContactRepository.getContactByName("John")
        assertTrue(contact?.autoApprove == true)
        assertTrue(viewModel.uiState.value.message?.contains("Auto-approve") == true)
    }

    @Test
    fun `setContactPriority updates contact priority`() = runTest {
        fakeContactRepository.addContact(
            ContactEntity(id = 1, name = "Alice", priority = "NORMAL")
        )
        viewModel = InboxViewModel(approveUseCase, rejectUseCase, fakeSuggestionDao, fakeContactRepository)
        advanceUntilIdle()

        viewModel.setContactPriority("Alice", ContactPriority.VIP)
        advanceUntilIdle()

        val contact = fakeContactRepository.getContactByName("Alice")
        assertEquals("VIP", contact?.priority)
        assertTrue(viewModel.uiState.value.message?.contains("VIP") == true)
    }

    @Test
    fun `WhatsApp suggestion with VIP has priority info in context`() = runTest {
        viewModel = InboxViewModel(approveUseCase, rejectUseCase, fakeSuggestionDao, fakeContactRepository)
        advanceUntilIdle()

        val suggestion = createWhatsAppSuggestion(
            title = "VIP Task",
            sender = "CEO",
            priority = ContactPriority.VIP
        )
        viewModel.addSuggestion(suggestion)

        val suggestions = viewModel.uiState.value.suggestions
        assertEquals(1, suggestions.size)
        assertEquals(ContactPriority.VIP, suggestions[0].whatsAppContext?.contactPriority)
    }

    @Test
    fun `viewModel without contact repository still works`() = runTest {
        viewModel = InboxViewModel(approveUseCase, rejectUseCase, fakeSuggestionDao, null)
        advanceUntilIdle()

        val suggestion = createWhatsAppSuggestion("Test task", "Someone")
        viewModel.addSuggestion(suggestion)

        // Should not crash even with null repository
        viewModel.setContactAutoApprove(suggestion, true)
        advanceUntilIdle()

        // No crash, message not set since contact not found
        assertEquals(1, viewModel.uiState.value.suggestions.size)
    }

    @Test
    fun `approveSuggestion works for WhatsApp suggestions`() = runTest {
        viewModel = InboxViewModel(approveUseCase, rejectUseCase, fakeSuggestionDao, fakeContactRepository)
        advanceUntilIdle()

        val suggestion = createWhatsAppSuggestion("WhatsApp Task", "Bob")
        viewModel.addSuggestion(suggestion)
        assertEquals(1, viewModel.uiState.value.suggestions.size)

        viewModel.approveSuggestion(suggestion)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.suggestions.isEmpty())
        assertEquals("Task approved: WhatsApp Task", viewModel.uiState.value.message)
    }

    private fun createWhatsAppSuggestion(
        title: String,
        sender: String,
        priority: ContactPriority = ContactPriority.NORMAL
    ) = TaskSuggestion(
        suggestedTitle = title,
        description = "Test description",
        priority = TaskPriority.MEDIUM,
        dueDate = null,
        sourceApp = "com.whatsapp",
        sender = sender,
        originalText = "Original WhatsApp message",
        confidence = 0.8f,
        whatsAppContext = WhatsAppContext(
            senderName = sender,
            messageSnippet = "Original WhatsApp message",
            timestamp = System.currentTimeMillis(),
            chatType = ChatType.PERSONAL,
            contactPriority = priority
        ),
        contactPriority = priority
    )

    private class FakeTaskSuggestionDaoForWhatsApp : TaskSuggestionDao {
        private val suggestions = MutableStateFlow<List<TaskSuggestionEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insert(suggestion: TaskSuggestionEntity): Long {
            val id = nextId++
            val entity = suggestion.copy(id = id)
            suggestions.value = suggestions.value + entity
            return id
        }

        override fun getAll(): Flow<List<TaskSuggestionEntity>> = suggestions

        override fun getByStatus(status: String): Flow<List<TaskSuggestionEntity>> =
            suggestions.map { list -> list.filter { it.status == status } }

        override suspend fun updateStatus(id: Long, status: String) {
            suggestions.value = suggestions.value.map {
                if (it.id == id) it.copy(status = status) else it
            }
        }

        override suspend fun delete(id: Long) {
            suggestions.value = suggestions.value.filter { it.id != id }
        }
    }

    private class FakeTaskRepositoryForWhatsApp : TaskRepository {
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

    private class FakeContactRepositoryForViewModel : ContactRepository {
        private val contacts = MutableStateFlow<List<ContactEntity>>(emptyList())
        private var nextId = 100L

        fun addContact(contact: ContactEntity) {
            contacts.value = contacts.value + contact
        }

        override fun getAllContacts(): Flow<List<ContactEntity>> = contacts

        override fun getEscalationContacts(): Flow<List<ContactEntity>> =
            contacts.map { list -> list.filter { it.isEscalationTarget } }

        override suspend fun getContactById(id: Long): ContactEntity? =
            contacts.value.find { it.id == id }

        override suspend fun getContactByName(name: String): ContactEntity? =
            contacts.value.find { it.name == name }

        override fun getContactsByPriority(priority: String): Flow<List<ContactEntity>> =
            contacts.map { list -> list.filter { it.priority == priority } }

        override suspend fun updatePriority(id: Long, priority: String) {
            contacts.value = contacts.value.map {
                if (it.id == id) it.copy(priority = priority) else it
            }
        }

        override suspend fun updateAutoApprove(id: Long, autoApprove: Boolean) {
            contacts.value = contacts.value.map {
                if (it.id == id) it.copy(autoApprove = autoApprove) else it
            }
        }

        override suspend fun incrementMessageCount(id: Long) {
            contacts.value = contacts.value.map {
                if (it.id == id) it.copy(messageCount = it.messageCount + 1) else it
            }
        }

        override suspend fun insertContact(contact: ContactEntity): Long {
            val id = nextId++
            contacts.value = contacts.value + contact.copy(id = id)
            return id
        }

        override suspend fun updateContact(contact: ContactEntity) {
            contacts.value = contacts.value.map {
                if (it.id == contact.id) contact else it
            }
        }

        override suspend fun deleteContact(contact: ContactEntity) {
            contacts.value = contacts.value.filter { it.id != contact.id }
        }
    }

    private class FakeLearningDataDaoForWhatsApp : LearningDataDao {
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

        override suspend fun getBySourceApp(app: String): List<LearningDataEntity> =
            data.filter { it.sourceApp == app }

        override suspend fun getBySender(sender: String): List<LearningDataEntity> =
            data.filter { it.sender == sender }

        override suspend fun getByFeedbackType(type: String): List<LearningDataEntity> =
            data.filter { it.feedbackType == type }

        override suspend fun getRecentDataList(limit: Int): List<LearningDataEntity> =
            data.sortedByDescending { it.timestamp }.take(limit)

        override suspend fun getCountByLabel(label: String): Int =
            data.count { it.label == label }

        override suspend fun deleteOldest(keepCount: Int) {
            if (data.size > keepCount) {
                val sorted = data.sortedByDescending { it.timestamp }
                data.clear()
                data.addAll(sorted.take(keepCount))
            }
        }
    }
}
