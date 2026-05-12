package com.procrastinationkiller.domain.engine.whatsapp

import com.procrastinationkiller.data.local.entity.ContactEntity
import com.procrastinationkiller.domain.model.ContactPriority
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WhatsAppIntelligenceEngineTest {

    private lateinit var engine: WhatsAppIntelligenceEngine
    private lateinit var fakeRepository: FakeContactRepository

    @BeforeEach
    fun setup() {
        fakeRepository = FakeContactRepository()
        engine = WhatsAppIntelligenceEngine(fakeRepository)
    }

    @Test
    fun `ignored contacts produce IgnoreResult`() = runBlocking {
        fakeRepository.addContact(
            ContactEntity(id = 1, name = "Spammer", priority = "IGNORE")
        )

        val result = engine.evaluate("Spammer", "Buy cheap stuff now!", false)

        assertTrue(result is WhatsAppEvaluationResult.IgnoreResult)
    }

    @Test
    fun `VIP contacts get HIGH priority override`() = runBlocking {
        fakeRepository.addContact(
            ContactEntity(id = 1, name = "Boss", priority = "VIP")
        )

        val result = engine.evaluate("Boss", "Please review the document", false)

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertEquals(TaskPriority.HIGH, processResult.priorityOverride)
        assertEquals(ContactPriority.VIP, processResult.contactPriority)
    }

    @Test
    fun `auto-approve contacts marked correctly`() = runBlocking {
        fakeRepository.addContact(
            ContactEntity(id = 1, name = "Trusted", priority = "NORMAL", autoApprove = true)
        )

        val result = engine.evaluate("Trusted", "Send me the report by Friday", false)

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertTrue(processResult.autoApprove)
    }

    @Test
    fun `group messages preserve context`() = runBlocking {
        fakeRepository.addContact(
            ContactEntity(id = 1, name = "Alice", priority = "NORMAL")
        )

        val result = engine.evaluate(
            sender = "Alice",
            message = "Can you finish the deployment by tomorrow?",
            isGroupChat = true,
            groupName = "Dev Team",
            timestamp = 1700000000L
        )

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertEquals(ChatType.GROUP, processResult.whatsAppContext.chatType)
        assertEquals("Dev Team", processResult.whatsAppContext.groupName)
        assertEquals("Alice", processResult.whatsAppContext.senderName)
        assertEquals(1700000000L, processResult.whatsAppContext.timestamp)
    }

    @Test
    fun `unknown contacts get NORMAL treatment`() = runBlocking {
        val result = engine.evaluate("NewPerson", "Please review my PR", false)

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertEquals(ContactPriority.NORMAL, processResult.contactPriority)
        assertNull(processResult.priorityOverride)
        assertFalse(processResult.autoApprove)
    }

    @Test
    fun `unknown contacts are created on first interaction`() = runBlocking {
        engine.evaluate("BrandNew", "Hello there", false, timestamp = 1700000000L)

        val created = fakeRepository.getContactByName("BrandNew")
        assertTrue(created != null)
        assertEquals("com.whatsapp", created?.sourceApp)
        assertEquals(1, created?.messageCount)
    }

    @Test
    fun `existing contact message count is incremented`() = runBlocking {
        fakeRepository.addContact(
            ContactEntity(id = 1, name = "Existing", priority = "NORMAL", messageCount = 5)
        )

        engine.evaluate("Existing", "Another message", false)

        assertTrue(fakeRepository.incrementedIds.contains(1L))
    }

    @Test
    fun `message snippet is truncated to 100 chars`() = runBlocking {
        val longMessage = "a".repeat(200)

        val result = engine.evaluate("Someone", longMessage, false)

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertEquals(100, processResult.whatsAppContext.messageSnippet.length)
    }

    @Test
    fun `HIGH_PRIORITY contacts do not get priority override`() = runBlocking {
        fakeRepository.addContact(
            ContactEntity(id = 1, name = "HighP", priority = "HIGH_PRIORITY")
        )

        val result = engine.evaluate("HighP", "Finish the task", false)

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertNull(processResult.priorityOverride)
        assertEquals(ContactPriority.HIGH_PRIORITY, processResult.contactPriority)
    }

    @Test
    fun `personal chat type set correctly`() = runBlocking {
        val result = engine.evaluate("Someone", "Hello", false)

        assertTrue(result is WhatsAppEvaluationResult.ProcessResult)
        val processResult = result as WhatsAppEvaluationResult.ProcessResult
        assertEquals(ChatType.PERSONAL, processResult.whatsAppContext.chatType)
        assertNull(processResult.whatsAppContext.groupName)
    }

    private class FakeContactRepository : ContactRepository {
        private val contacts = MutableStateFlow<List<ContactEntity>>(emptyList())
        val incrementedIds = mutableListOf<Long>()
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
            incrementedIds.add(id)
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
}
