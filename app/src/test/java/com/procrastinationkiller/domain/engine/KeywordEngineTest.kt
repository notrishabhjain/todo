package com.procrastinationkiller.domain.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Calendar

class KeywordEngineTest {

    private lateinit var keywordEngine: KeywordEngine

    @BeforeEach
    fun setup() {
        keywordEngine = KeywordEngine()
    }

    @Test
    fun `detects English action keywords`() {
        val analysis = keywordEngine.analyze("Please send the report by end of day")
        assertTrue(analysis.isActionable)
        assertTrue(analysis.actionKeywords.any { it.keyword == "send" })
        assertEquals(Language.ENGLISH, analysis.actionKeywords.first { it.keyword == "send" }.language)
    }

    @Test
    fun `detects multiple English action keywords`() {
        val analysis = keywordEngine.analyze("Review and submit the PR before deploying")
        assertTrue(analysis.isActionable)
        assertTrue(analysis.actionKeywords.any { it.keyword == "review" })
        assertTrue(analysis.actionKeywords.any { it.keyword == "submit" })
    }

    @Test
    fun `detects Hindi Hinglish action keywords`() {
        val analysis = keywordEngine.analyze("Bhai kal tak proposal bhej dena")
        assertTrue(analysis.isActionable)
        assertTrue(analysis.actionKeywords.any { it.keyword == "bhej dena" })
        assertEquals(Language.HINGLISH, analysis.actionKeywords.first { it.keyword == "bhej dena" }.language)
    }

    @Test
    fun `detects Hinglish karna keyword`() {
        val analysis = keywordEngine.analyze("Yeh kaam aaj karna hai")
        assertTrue(analysis.isActionable)
        assertTrue(analysis.actionKeywords.any { it.keyword == "karna" })
    }

    @Test
    fun `detects English urgency keywords`() {
        val analysis = keywordEngine.analyze("This is urgent, please complete the task")
        assertTrue(analysis.urgencyKeywords.any { it.keyword == "urgent" })
        assertEquals(KeywordCategory.URGENCY, analysis.urgencyKeywords.first().category)
    }

    @Test
    fun `detects Hindi urgency keywords`() {
        val analysis = keywordEngine.analyze("Jaldi kar do, bahut zaruri hai")
        assertTrue(analysis.urgencyKeywords.any { it.keyword == "jaldi" })
    }

    @Test
    fun `detects English time indicators`() {
        val analysis = keywordEngine.analyze("Send the report by tomorrow")
        assertTrue(analysis.timeIndicators.any { it.keyword == "tomorrow" })
        assertNotNull(analysis.resolvedDueDate)
    }

    @Test
    fun `detects Hindi time indicator kal`() {
        val analysis = keywordEngine.analyze("Kal tak bhej dena")
        assertTrue(analysis.timeIndicators.any { it.keyword == "kal" || it.keyword == "kal tak" })
        assertNotNull(analysis.resolvedDueDate)
    }

    @Test
    fun `resolves tomorrow to next day`() {
        val analysis = keywordEngine.analyze("Do it tomorrow")
        assertNotNull(analysis.resolvedDueDate)

        val expectedCal = Calendar.getInstance()
        expectedCal.add(Calendar.DAY_OF_YEAR, 1)

        val resolvedCal = Calendar.getInstance()
        resolvedCal.timeInMillis = analysis.resolvedDueDate!!

        assertEquals(expectedCal.get(Calendar.DAY_OF_YEAR), resolvedCal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `resolves next week correctly`() {
        val analysis = keywordEngine.analyze("Finish this next week")
        assertNotNull(analysis.resolvedDueDate)

        val expectedCal = Calendar.getInstance()
        expectedCal.add(Calendar.WEEK_OF_YEAR, 1)

        val resolvedCal = Calendar.getInstance()
        resolvedCal.timeInMillis = analysis.resolvedDueDate!!

        assertEquals(expectedCal.get(Calendar.WEEK_OF_YEAR), resolvedCal.get(Calendar.WEEK_OF_YEAR))
    }

    @Test
    fun `non-actionable text returns isActionable false`() {
        val analysis = keywordEngine.analyze("Good morning! How are you?")
        assertFalse(analysis.isActionable)
        assertTrue(analysis.actionKeywords.isEmpty())
    }

    @Test
    fun `no time indicators returns null dueDate`() {
        val analysis = keywordEngine.analyze("Please send the file")
        assertNull(analysis.resolvedDueDate)
    }

    @Test
    fun `handles empty text`() {
        val analysis = keywordEngine.analyze("")
        assertFalse(analysis.isActionable)
        assertNull(analysis.resolvedDueDate)
    }

    @Test
    fun `detects eod as time indicator`() {
        val analysis = keywordEngine.analyze("Complete this by EOD")
        assertTrue(analysis.timeIndicators.any { it.keyword == "eod" })
        assertNotNull(analysis.resolvedDueDate)
    }

    @Test
    fun `Hinglish phrase with multiple signals`() {
        val analysis = keywordEngine.analyze("Bhai jaldi check karna aaj hi")
        assertTrue(analysis.isActionable)
        assertTrue(analysis.urgencyKeywords.isNotEmpty())
        assertTrue(analysis.actionKeywords.any { it.keyword == "check karna" })
    }
}
