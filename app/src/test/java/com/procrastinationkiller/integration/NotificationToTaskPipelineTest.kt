package com.procrastinationkiller.integration

import com.procrastinationkiller.data.parser.AppType
import com.procrastinationkiller.data.parser.NotificationParser
import com.procrastinationkiller.domain.engine.KeywordEngine
import com.procrastinationkiller.domain.engine.TaskExtractionEngine
import com.procrastinationkiller.domain.model.TaskPriority
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NotificationToTaskPipelineTest {

    private lateinit var keywordEngine: KeywordEngine
    private lateinit var extractionEngine: TaskExtractionEngine
    private lateinit var parser: NotificationParser

    @BeforeEach
    fun setup() {
        keywordEngine = KeywordEngine()
        extractionEngine = TaskExtractionEngine(keywordEngine)
        parser = NotificationParser()
    }

    @Test
    fun `English actionable notification produces task suggestion`() = runBlocking {
        val text = "Please send the quarterly report by end of day"
        val suggestion = extractionEngine.extract(text, "com.whatsapp", "Boss")

        assertNotNull(suggestion)
        assertTrue(suggestion!!.suggestedTitle.isNotEmpty())
        assertEquals("com.whatsapp", suggestion.sourceApp)
        assertEquals("Boss", suggestion.sender)
        assertNotNull(suggestion.dueDate)
    }

    @Test
    fun `Hindi actionable notification produces task suggestion`() = runBlocking {
        val text = "Bhai kal tak report bhej dena jaldi"
        val suggestion = extractionEngine.extract(text, "com.whatsapp", "Rahul")

        assertNotNull(suggestion)
        assertTrue(suggestion!!.confidence > 0.3f)
        assertNotNull(suggestion.dueDate)
    }

    @Test
    fun `Hinglish urgent notification gets high priority`() = runBlocking {
        val text = "Urgent hai, deploy karna hai aaj hi"
        val suggestion = extractionEngine.extract(text, "com.slack", "TeamLead")

        assertNotNull(suggestion)
        assertTrue(suggestion!!.priority == TaskPriority.HIGH || suggestion.priority == TaskPriority.CRITICAL)
    }

    @Test
    fun `Non-actionable notification returns null`() = runBlocking {
        val text = "Good morning! Hope you have a great day"
        val suggestion = extractionEngine.extract(text, "com.whatsapp", "Friend")

        assertNull(suggestion)
    }

    @Test
    fun `Multiple action keywords increase confidence`() = runBlocking {
        val text = "Review and submit the PR before deploying to production"
        val suggestion = extractionEngine.extract(text, "com.github", "CI Bot")

        assertNotNull(suggestion)
        assertTrue(suggestion!!.confidence >= 0.3f)
    }

    @Test
    fun `Notification with time indicator gets due date`() = runBlocking {
        val text = "Please complete the task by tomorrow morning"
        val suggestion = extractionEngine.extract(text, "com.gmail", "Manager")

        assertNotNull(suggestion)
        assertNotNull(suggestion!!.dueDate)
    }

    @Test
    fun `Full pipeline from notification text to task fields`() = runBlocking {
        val notifText = "Hey, can you review the design docs and send feedback by Friday?"
        val sender = parser.extractSender("Design Team", notifText, AppType.WHATSAPP, false)

        val suggestion = extractionEngine.extract(
            text = notifText,
            sourceApp = "com.whatsapp",
            sender = sender
        )

        assertNotNull(suggestion)
        assertEquals("com.whatsapp", suggestion!!.sourceApp)
        assertTrue(suggestion.suggestedTitle.isNotEmpty())
    }

    @Test
    fun `Hinglish mixed text extracts correctly`() = runBlocking {
        val text = "Yaar check karna database issue, shaam tak fix kar dena"
        val suggestion = extractionEngine.extract(text, "com.telegram", "DevOps")

        assertNotNull(suggestion)
        assertTrue(suggestion!!.priority == TaskPriority.HIGH || suggestion.priority == TaskPriority.MEDIUM)
    }
}
