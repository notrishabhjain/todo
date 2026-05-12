package com.procrastinationkiller.domain.engine

import com.procrastinationkiller.domain.model.TaskPriority
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TranscriptAnalyzerTest {

    private lateinit var transcriptAnalyzer: TranscriptAnalyzer
    private lateinit var keywordEngine: KeywordEngine

    @BeforeEach
    fun setup() {
        keywordEngine = KeywordEngine()
        transcriptAnalyzer = TranscriptAnalyzer(keywordEngine)
    }

    @Test
    fun `extracts action items from English transcript`() {
        val transcript = """
            John: We need to review the PR before end of day
            Sarah: I will send the report tomorrow
            Mike: Sounds good, let's move on
        """.trimIndent()

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.text.contains("review") })
        assertTrue(items.any { it.text.contains("send") })
    }

    @Test
    fun `extracts action items from Hindi transcript`() {
        val transcript = """
            Rahul: Kal tak report bhej dena
            Priya: Check karna hai database issue
        """.trimIndent()

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
    }

    @Test
    fun `extracts action items from Hinglish transcript`() {
        val transcript = """
            Dev: Yeh PR review karna hai jaldi
            Neha: OK, kal tak kar dena
        """.trimIndent()

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
    }

    @Test
    fun `detects owner from at mention`() {
        val transcript = "@John please send the report by tomorrow"

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertEquals("John", items.first().owner)
    }

    @Test
    fun `detects owner from assignment pattern`() {
        val transcript = "Sarah, please review the document"

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertEquals("Sarah", items.first().owner)
    }

    @Test
    fun `detects speaker as owner when no explicit assignment`() {
        val transcript = "Alice: I need to fix the login bug"

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertEquals("Alice", items.first().owner)
    }

    @Test
    fun `non-actionable text produces no items`() {
        val transcript = """
            John: Good morning everyone
            Sarah: How was the weekend?
            Mike: Pretty good thanks
        """.trimIndent()

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isEmpty())
    }

    @Test
    fun `sets high priority for urgent items`() {
        val transcript = "Manager: This is urgent, deploy the fix immediately"

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.priority == TaskPriority.HIGH || it.priority == TaskPriority.CRITICAL })
    }

    @Test
    fun `resolves due date from time indicators`() {
        val transcript = "Boss: Submit the report by tomorrow"

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertNotNull(items.first().dueDate)
    }

    @Test
    fun `handles empty transcript`() {
        val items = transcriptAnalyzer.analyze("")
        assertTrue(items.isEmpty())
    }

    @Test
    fun `handles multi-line without speakers`() {
        val transcript = """
            Please send the invoice
            Fix the login page issue
            Update the documentation
        """.trimIndent()

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
    }

    @Test
    fun `detects owner from Hindi assignment`() {
        val transcript = "Rahul ko bhej dena report kal tak"

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertEquals("Rahul", items.first().owner)
    }
}
