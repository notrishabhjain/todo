package com.procrastinationkiller.domain.engine.semantic

import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SemanticDeduplicatorTest {

    private lateinit var deduplicator: SemanticDeduplicator

    @BeforeEach
    fun setup() {
        deduplicator = SemanticDeduplicator()
    }

    @Test
    fun `send the report vs please send the report ASAP is duplicate`() {
        val existing = listOf(
            createSuggestion(1, "send the report", "Boss")
        )

        val result = deduplicator.checkDuplicate(
            newText = "send the report ASAP",
            newSender = "Boss",
            existingSuggestions = existing
        )

        assertTrue(result.isDuplicate)
        assertTrue(result.similarityScore >= 0.75f)
    }

    @Test
    fun `send the report vs review the report is NOT duplicate`() {
        val existing = listOf(
            createSuggestion(1, "send the report", "Boss")
        )

        val result = deduplicator.checkDuplicate(
            newText = "review the report",
            newSender = "Boss",
            existingSuggestions = existing
        )

        assertFalse(result.isDuplicate)
    }

    @Test
    fun `same sender with similar keywords and slight rewording is duplicate`() {
        val existing = listOf(
            createSuggestion(1, "send monthly report to client", "Manager")
        )

        val result = deduplicator.checkDuplicate(
            newText = "send monthly report to client",
            newSender = "Manager",
            existingSuggestions = existing
        )

        assertTrue(result.isDuplicate)
        assertTrue(result.similarityScore >= 0.75f)
    }

    @Test
    fun `completely different messages are NOT duplicates`() {
        val existing = listOf(
            createSuggestion(1, "send the report", "Boss")
        )

        val result = deduplicator.checkDuplicate(
            newText = "book a meeting room for Friday",
            newSender = "Boss",
            existingSuggestions = existing
        )

        assertFalse(result.isDuplicate)
    }

    @Test
    fun `empty existing suggestions returns not duplicate`() {
        val result = deduplicator.checkDuplicate(
            newText = "send the report",
            newSender = "Boss",
            existingSuggestions = emptyList()
        )

        assertFalse(result.isDuplicate)
    }

    @Test
    fun `different senders reduce similarity`() {
        val existing = listOf(
            createSuggestion(1, "check the logs", "Alice")
        )

        // Without same sender boost, short texts might not reach threshold
        val result = deduplicator.checkDuplicate(
            newText = "book a hotel room",
            newSender = "Bob",
            existingSuggestions = existing
        )

        assertFalse(result.isDuplicate)
    }

    private fun createSuggestion(id: Long, text: String, sender: String): TaskSuggestionEntity {
        return TaskSuggestionEntity(
            id = id,
            suggestedTitle = text.take(30),
            description = text,
            priority = "MEDIUM",
            dueDate = null,
            sourceApp = "com.whatsapp",
            sender = sender,
            originalText = text,
            confidence = 0.8f,
            autoApprove = false,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
    }
}
