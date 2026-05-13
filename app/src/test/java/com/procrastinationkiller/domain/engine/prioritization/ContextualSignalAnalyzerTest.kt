package com.procrastinationkiller.domain.engine.prioritization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContextualSignalAnalyzerTest {

    private lateinit var analyzer: ContextualSignalAnalyzer

    @BeforeEach
    fun setup() {
        analyzer = ContextualSignalAnalyzer()
    }

    @Test
    fun `ALL CAPS message returns high signal`() {
        val result = analyzer.analyze("SEND THE REPORT NOW")

        assertTrue(result.signalScore > 0f)
        assertTrue(result.factors.contains("ALL_CAPS_DETECTED"))
    }

    @Test
    fun `multiple exclamation marks increase urgency`() {
        val result = analyzer.analyze("Send it now!!!")

        assertTrue(result.signalScore > 0f)
        assertTrue(result.factors.contains("EXCLAMATION_MARKS"))
    }

    @Test
    fun `single exclamation mark has lower score`() {
        val singleResult = analyzer.analyze("Please send it!")
        val multipleResult = analyzer.analyze("Please send it!!!")

        assertTrue(multipleResult.signalScore > singleResult.signalScore)
    }

    @Test
    fun `repeated messages from same sender escalate`() {
        val result = analyzer.analyze(
            text = "Did you see my message?",
            sender = "Boss",
            recentMessagesFromSender = 4
        )

        assertTrue(result.signalScore > 0f)
        assertTrue(result.factors.contains("REPEATED_MESSAGES"))
    }

    @Test
    fun `no repeated messages gives no repeated factor`() {
        val result = analyzer.analyze(
            text = "Please check the logs",
            sender = "Colleague",
            recentMessagesFromSender = 1
        )

        assertTrue(!result.factors.contains("REPEATED_MESSAGES"))
    }

    @Test
    fun `calm lowercase message returns zero signal`() {
        val result = analyzer.analyze("can you review the document when free")

        assertEquals(0f, result.signalScore)
        assertTrue(result.factors.isEmpty())
    }

    @Test
    fun `mixed case text below threshold is not flagged`() {
        val result = analyzer.analyze("Please Send the Report by tomorrow")

        assertTrue(!result.factors.contains("ALL_CAPS_DETECTED"))
    }

    @Test
    fun `short text is not flagged for caps`() {
        val result = analyzer.analyze("OK")

        assertTrue(!result.factors.contains("ALL_CAPS_DETECTED"))
    }

    @Test
    fun `combination of signals gives highest score`() {
        val combinedResult = analyzer.analyze(
            text = "URGENT!!! SEND IT NOW!!!",
            sender = "Boss",
            recentMessagesFromSender = 3
        )

        val singleResult = analyzer.analyze("please send the report")

        assertTrue(combinedResult.signalScore > singleResult.signalScore)
        assertTrue(combinedResult.factors.size >= 2)
    }

    @Test
    fun `signal score is capped at 1`() {
        val result = analyzer.analyze(
            text = "SEND IT!!!! NOW!!!! URGENT!!!! ASAP!!!!",
            sender = "Boss",
            recentMessagesFromSender = 10
        )

        assertTrue(result.signalScore <= 1.0f)
    }
}
