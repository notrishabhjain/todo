package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.entity.ContactEntity
import com.procrastinationkiller.domain.engine.whatsapp.WhatsAppEvaluationResult
import com.procrastinationkiller.domain.engine.whatsapp.WhatsAppIntelligenceEngine
import com.procrastinationkiller.domain.model.ContactPriority
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TaskExtractionUseCaseDeduplicationTest {

    private lateinit var fakeContactRepository: FakeContactRepository
    private lateinit var whatsAppIntelligenceEngine: WhatsAppIntelligenceEngine

    @BeforeEach
    fun setup() {
        fakeContactRepository = FakeContactRepository()
        whatsAppIntelligenceEngine = WhatsAppIntelligenceEngine(fakeContactRepository)
    }

    @Test
    fun `VIP contact results in autoApprove true`() = runBlocking {
        // Add a VIP contact
        fakeContactRepository.contacts.add(
            ContactEntity(
                id = 1,
                name = "VIP Boss",
                priority = "VIP",
                autoApprove = false, // Even without autoApprove field set, VIP should auto-approve
                sourceApp = "com.whatsapp",
                messageCount = 5
            )
        )

        val result = whatsAppIntelligenceEngine.evaluate(
            sender = "VIP Boss",
            message = "Please submit the quarterly report by Friday",
            isGroupChat = false,
            groupName = null,
            timestamp = System.currentTimeMillis()
        )

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertTrue(processResult.autoApprove)
        assertEquals(ContactPriority.VIP, processResult.contactPriority)
        assertEquals(TaskPriority.HIGH, processResult.priorityOverride)
    }

    @Test
    fun `VIP contact with autoApprove field false still gets autoApprove true`() = runBlocking {
        fakeContactRepository.contacts.add(
            ContactEntity(
                id = 1,
                name = "Important Person",
                priority = "VIP",
                autoApprove = false,
                sourceApp = "com.whatsapp",
                messageCount = 10
            )
        )

        val result = whatsAppIntelligenceEngine.evaluate(
            sender = "Important Person",
            message = "Call me about the project",
            isGroupChat = false,
            timestamp = System.currentTimeMillis()
        )

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertTrue(processResult.autoApprove)
    }

    @Test
    fun `NORMAL contact without autoApprove field does not get autoApprove`() = runBlocking {
        fakeContactRepository.contacts.add(
            ContactEntity(
                id = 1,
                name = "Regular Person",
                priority = "NORMAL",
                autoApprove = false,
                sourceApp = "com.whatsapp",
                messageCount = 3
            )
        )

        val result = whatsAppIntelligenceEngine.evaluate(
            sender = "Regular Person",
            message = "Can you send me a file?",
            isGroupChat = false,
            timestamp = System.currentTimeMillis()
        )

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertFalse(processResult.autoApprove)
    }

    @Test
    fun `NORMAL contact with autoApprove field true gets autoApprove`() = runBlocking {
        fakeContactRepository.contacts.add(
            ContactEntity(
                id = 1,
                name = "Trusted Friend",
                priority = "NORMAL",
                autoApprove = true,
                sourceApp = "com.whatsapp",
                messageCount = 20
            )
        )

        val result = whatsAppIntelligenceEngine.evaluate(
            sender = "Trusted Friend",
            message = "Pick up groceries on the way home",
            isGroupChat = false,
            timestamp = System.currentTimeMillis()
        )

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertTrue(processResult.autoApprove)
    }

    @Test
    fun `IGNORE contact is skipped`() = runBlocking {
        fakeContactRepository.contacts.add(
            ContactEntity(
                id = 1,
                name = "Spammer",
                priority = "IGNORE",
                autoApprove = false,
                sourceApp = "com.whatsapp",
                messageCount = 50
            )
        )

        val result = whatsAppIntelligenceEngine.evaluate(
            sender = "Spammer",
            message = "Buy this product now!",
            isGroupChat = false,
            timestamp = System.currentTimeMillis()
        )

        assertTrue(result is WhatsAppEvaluationResult.IgnoreResult)
    }

    @Test
    fun `computeContentHash produces consistent results for same input`() {
        val hash1 = TaskExtractionUseCase.computeContentHash("Alice", "Send report", "com.whatsapp")
        val hash2 = TaskExtractionUseCase.computeContentHash("Alice", "Send report", "com.whatsapp")

        assertEquals(hash1, hash2)
    }

    @Test
    fun `computeContentHash produces different results for different sender`() {
        val hash1 = TaskExtractionUseCase.computeContentHash("Alice", "Send report", "com.whatsapp")
        val hash2 = TaskExtractionUseCase.computeContentHash("Bob", "Send report", "com.whatsapp")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `computeContentHash produces different results for different content`() {
        val hash1 = TaskExtractionUseCase.computeContentHash("Alice", "Send report", "com.whatsapp")
        val hash2 = TaskExtractionUseCase.computeContentHash("Alice", "Review document", "com.whatsapp")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `computeContentHash produces different results for different source app`() {
        val hash1 = TaskExtractionUseCase.computeContentHash("Alice", "Send report", "com.whatsapp")
        val hash2 = TaskExtractionUseCase.computeContentHash("Alice", "Send report", "com.slack")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `computeNotificationKey produces consistent results for same input`() {
        val key1 = TaskExtractionUseCase.computeNotificationKey("com.whatsapp", "Alice", "Hello")
        val key2 = TaskExtractionUseCase.computeNotificationKey("com.whatsapp", "Alice", "Hello")

        assertEquals(key1, key2)
    }

    @Test
    fun `computeNotificationKey produces different results for different content`() {
        val key1 = TaskExtractionUseCase.computeNotificationKey("com.whatsapp", "Alice", "Hello")
        val key2 = TaskExtractionUseCase.computeNotificationKey("com.whatsapp", "Alice", "Goodbye")

        assertNotEquals(key1, key2)
    }

    @Test
    fun `computeNotificationKey produces different results for different package`() {
        val key1 = TaskExtractionUseCase.computeNotificationKey("com.whatsapp", "Alice", "Hello")
        val key2 = TaskExtractionUseCase.computeNotificationKey("com.telegram", "Alice", "Hello")

        assertNotEquals(key1, key2)
    }

    @Test
    fun `content hash is a valid SHA-256 hex string`() {
        val hash = TaskExtractionUseCase.computeContentHash("sender", "text", "app")

        // SHA-256 produces 64 hex characters
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `notification key is a valid SHA-256 hex string`() {
        val key = TaskExtractionUseCase.computeNotificationKey("package", "title", "content")

        // SHA-256 produces 64 hex characters
        assertEquals(64, key.length)
        assertTrue(key.matches(Regex("[0-9a-f]{64}")))
    }

    // --- Fake ContactRepository ---
    private class FakeContactRepository : ContactRepository {
        val contacts = mutableListOf<ContactEntity>()
        private var nextId = 100L

        override fun getAllContacts(): Flow<List<ContactEntity>> = MutableStateFlow(contacts.toList())
        override fun getEscalationContacts(): Flow<List<ContactEntity>> =
            MutableStateFlow(contacts.filter { it.isEscalationTarget })
        override suspend fun getContactById(id: Long): ContactEntity? = contacts.find { it.id == id }
        override suspend fun getContactByName(name: String): ContactEntity? =
            contacts.find { it.name == name }
        override fun getContactsByPriority(priority: String): Flow<List<ContactEntity>> =
            MutableStateFlow(contacts.filter { it.priority == priority })
        override suspend fun updatePriority(id: Long, priority: String) {
            val index = contacts.indexOfFirst { it.id == id }
            if (index >= 0) contacts[index] = contacts[index].copy(priority = priority)
        }
        override suspend fun updateAutoApprove(id: Long, autoApprove: Boolean) {
            val index = contacts.indexOfFirst { it.id == id }
            if (index >= 0) contacts[index] = contacts[index].copy(autoApprove = autoApprove)
        }
        override suspend fun incrementMessageCount(id: Long) {
            val index = contacts.indexOfFirst { it.id == id }
            if (index >= 0) contacts[index] = contacts[index].copy(messageCount = contacts[index].messageCount + 1)
        }
        override suspend fun insertContact(contact: ContactEntity): Long {
            val id = nextId++
            contacts.add(contact.copy(id = id))
            return id
        }
        override suspend fun updateContact(contact: ContactEntity) {
            val index = contacts.indexOfFirst { it.id == contact.id }
            if (index >= 0) contacts[index] = contact
        }
        override suspend fun deleteContact(contact: ContactEntity) {
            contacts.removeAll { it.id == contact.id }
        }
    }
}
