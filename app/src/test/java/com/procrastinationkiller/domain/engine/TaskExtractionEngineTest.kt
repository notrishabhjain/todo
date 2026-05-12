package com.procrastinationkiller.domain.engine

import com.procrastinationkiller.domain.model.TaskPriority
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Calendar

class TaskExtractionEngineTest {

    private lateinit var keywordEngine: KeywordEngine
    private lateinit var extractionEngine: TaskExtractionEngine

    @BeforeEach
    fun setup() {
        keywordEngine = KeywordEngine()
        extractionEngine = TaskExtractionEngine(keywordEngine)
    }

    @Test
    fun `extracts task from Hinglish message with time indicator`() = runBlocking {
        val suggestion = extractionEngine.extract(
            text = "Bhai kal tak proposal bhej dena",
            sourceApp = "com.whatsapp",
            sender = "Rahul"
        )

        assertNotNull(suggestion)
        suggestion!!

        // Title should reference the action
        assertTrue(
            suggestion.suggestedTitle.lowercase().contains("proposal") ||
                suggestion.suggestedTitle.lowercase().contains("bhej")
        )

        // Priority should be HIGH due to time indicator "kal tak"
        assertEquals(TaskPriority.HIGH, suggestion.priority)

        // Due date should be tomorrow
        assertNotNull(suggestion.dueDate)
        val expectedCal = Calendar.getInstance()
        expectedCal.add(Calendar.DAY_OF_YEAR, 1)
        val resolvedCal = Calendar.getInstance()
        resolvedCal.timeInMillis = suggestion.dueDate!!
        assertEquals(expectedCal.get(Calendar.DAY_OF_YEAR), resolvedCal.get(Calendar.DAY_OF_YEAR))

        assertEquals("com.whatsapp", suggestion.sourceApp)
        assertEquals("Rahul", suggestion.sender)
    }

    @Test
    fun `extracts task from English urgent message`() = runBlocking {
        val suggestion = extractionEngine.extract(
            text = "Urgent: Please review the PR and deploy to staging",
            sourceApp = "com.slack",
            sender = "Manager"
        )

        assertNotNull(suggestion)
        suggestion!!

        assertTrue(suggestion.priority == TaskPriority.HIGH || suggestion.priority == TaskPriority.CRITICAL)
        assertTrue(suggestion.confidence > 0.0f)
    }

    @Test
    fun `returns null for non-actionable message`() = runBlocking {
        val suggestion = extractionEngine.extract(
            text = "Good morning everyone!",
            sourceApp = "com.whatsapp",
            sender = "Friend"
        )

        assertNull(suggestion)
    }

    @Test
    fun `assigns CRITICAL priority with multiple urgency indicators`() = runBlocking {
        val suggestion = extractionEngine.extract(
            text = "Urgent! Send the report ASAP, this is high priority",
            sourceApp = "com.google.android.gm",
            sender = "Boss"
        )

        assertNotNull(suggestion)
        suggestion!!
        assertEquals(TaskPriority.CRITICAL, suggestion.priority)
    }

    @Test
    fun `assigns MEDIUM priority with no urgency and no time`() = runBlocking {
        val suggestion = extractionEngine.extract(
            text = "Can you check the logs when you get a chance",
            sourceApp = "com.slack",
            sender = "Colleague"
        )

        assertNotNull(suggestion)
        suggestion!!
        assertEquals(TaskPriority.MEDIUM, suggestion.priority)
    }

    @Test
    fun `preserves original text in suggestion`() = runBlocking {
        val originalText = "Please submit the document by tomorrow"
        val suggestion = extractionEngine.extract(
            text = originalText,
            sourceApp = "com.google.android.gm",
            sender = "Admin"
        )

        assertNotNull(suggestion)
        suggestion!!
        assertEquals(originalText, suggestion.originalText)
    }

    @Test
    fun `confidence score increases with more signals`() = runBlocking {
        val weakSuggestion = extractionEngine.extract(
            text = "Send the file",
            sourceApp = "com.whatsapp",
            sender = "A"
        )

        val strongSuggestion = extractionEngine.extract(
            text = "Urgent! Send the report by tomorrow ASAP",
            sourceApp = "com.whatsapp",
            sender = "B"
        )

        assertNotNull(weakSuggestion)
        assertNotNull(strongSuggestion)
        assertTrue(strongSuggestion!!.confidence > weakSuggestion!!.confidence)
    }

    @Test
    fun `extracts task from Hinglish with urgency`() = runBlocking {
        val suggestion = extractionEngine.extract(
            text = "Jaldi check karna aaj hi report",
            sourceApp = "com.whatsapp",
            sender = "Team Lead"
        )

        assertNotNull(suggestion)
        suggestion!!
        assertTrue(suggestion.priority == TaskPriority.HIGH || suggestion.priority == TaskPriority.CRITICAL)
    }

    @Test
    fun `handles empty text gracefully`() = runBlocking {
        val suggestion = extractionEngine.extract(
            text = "",
            sourceApp = "com.whatsapp",
            sender = "Unknown"
        )

        assertNull(suggestion)
    }
}
