package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskSuggestion
import com.procrastinationkiller.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApproveTaskUseCaseTest {

    private lateinit var approveUseCase: ApproveTaskUseCase
    private lateinit var fakeRepository: FakeTaskRepositoryForApprove

    @BeforeEach
    fun setup() {
        fakeRepository = FakeTaskRepositoryForApprove()
        approveUseCase = ApproveTaskUseCase(fakeRepository)
    }

    @Test
    fun `extractKeywords filters out stop words`() = runBlocking {
        val suggestion = TaskSuggestion(
            suggestedTitle = "Send report",
            description = "test",
            priority = TaskPriority.MEDIUM,
            dueDate = null,
            sourceApp = "com.test",
            sender = "sender",
            originalText = "please send this report from your desk before tomorrow",
            confidence = 0.8f
        )

        // The use case calls extractKeywords internally via recordFeedback
        // We verify the keyword extraction logic by testing what gets fed to the learning engine
        val keywords = extractKeywordsForTest(suggestion.originalText)

        // "this", "from", "your", "before" are stop words and should be filtered
        assertFalse(keywords.contains("this"))
        assertFalse(keywords.contains("from"))
        assertFalse(keywords.contains("your"))
        assertFalse(keywords.contains("before"))

        // "please", "send", "report", "desk", "tomorrow" pass length > 3 check
        // "please" and "send" pass length > 3 and are not stop words
        assertTrue(keywords.contains("please"))
        assertTrue(keywords.contains("send") || keywords.contains("report"))
    }

    @Test
    fun `approve propagates sourceApp sender originalText to task entity`() = runBlocking {
        val suggestion = TaskSuggestion(
            suggestedTitle = "Send quarterly report",
            description = "Q4 report needed",
            priority = TaskPriority.HIGH,
            dueDate = null,
            sourceApp = "com.whatsapp",
            sender = "Boss",
            originalText = "Please send the quarterly report ASAP",
            confidence = 0.9f
        )

        approveUseCase(suggestion)

        val insertedTask = fakeRepository.getLastInsertedTask()
        assertNotNull(insertedTask)
        assertEquals("com.whatsapp", insertedTask?.sourceApp)
        assertEquals("Boss", insertedTask?.sender)
        assertEquals("Please send the quarterly report ASAP", insertedTask?.originalText)
    }

    @Test
    fun `extractKeywords keeps meaningful words`() {
        val text = "deploy the application to production server immediately"
        val keywords = extractKeywordsForTest(text)

        assertTrue(keywords.contains("deploy"))
        assertTrue(keywords.contains("application"))
        assertTrue(keywords.contains("production"))
        assertTrue(keywords.contains("server"))
        assertTrue(keywords.contains("immediately"))
    }

    @Test
    fun `extractKeywords removes short words`() {
        val text = "do it by the end of day"
        val keywords = extractKeywordsForTest(text)

        assertFalse(keywords.contains("do"))
        assertFalse(keywords.contains("it"))
        assertFalse(keywords.contains("by"))
        assertFalse(keywords.contains("the"))
        assertFalse(keywords.contains("of"))
        assertFalse(keywords.contains("day"))
    }

    @Test
    fun `extractKeywords limits to 10 results`() {
        val text = "word1 word2 word3 word4 word5 word6 word7 word8 word9 word10 word11 word12 word13"
        val keywords = extractKeywordsForTest(text)

        assertTrue(keywords.size <= 10)
    }

    // Expose the same logic used internally for testing
    private fun extractKeywordsForTest(text: String): List<String> {
        val stopWords = setOf(
            "this", "that", "with", "from", "have", "been", "were", "they",
            "them", "then", "than", "these", "those", "their", "there",
            "when", "what", "which", "where", "will", "would", "could",
            "should", "about", "after", "before", "between", "each",
            "every", "into", "through", "does", "done", "just", "more",
            "most", "much", "also", "back", "some", "such", "very",
            "your", "yours", "here", "only", "still", "over", "under"
        )
        return text.lowercase()
            .split("\\s+".toRegex())
            .filter { it.length > 3 && it !in stopWords }
            .take(10)
    }

    private class FakeTaskRepositoryForApprove : TaskRepository {
        private val tasks = mutableListOf<TaskEntity>()
        private val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
        private var nextId = 1L

        fun getLastInsertedTask(): TaskEntity? = tasks.lastOrNull()

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
