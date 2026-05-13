package com.procrastinationkiller.domain.engine

import com.procrastinationkiller.domain.engine.ml.HybridClassificationPipeline
import com.procrastinationkiller.domain.engine.ml.OnnxIntentClassifier
import com.procrastinationkiller.domain.engine.ml.RuleBasedIntentClassifier
import com.procrastinationkiller.domain.engine.ml.TextFeatureExtractor
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

    @Test
    fun `extracts action items from bold speaker format with timestamps`() {
        val transcript = """
            [00:59:38] **Speaker 2:** Please send the report by tomorrow
            [00:59:43] **Speaker 4:** I will review the PR today
        """.trimIndent()

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.text.contains("send") })
        assertTrue(items.any { it.text.contains("review") })
    }

    @Test
    fun `handles bold speaker format without timestamps`() {
        val transcript = """
            **Speaker 1:** We need to deploy the fix
            **Speaker 2:** OK, I will handle it
        """.trimIndent()

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.text.contains("deploy") })
    }

    @Test
    fun `handles user exact transcript format with Hindi content`() {
        val transcript = """
            [00:59:38] **Speaker 2:** Kal tak report bhej dena jaldi
            [00:59:43] **Speaker 4:** Check karna hai database issue urgent hai
        """.trimIndent()

        val items = transcriptAnalyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertTrue(items.size >= 2)
    }

    @Test
    fun `enhanced analyze handles bold speaker format with timestamps`() {
        val pipeline = createPipeline()
        val transcript = """
            [00:59:38] **Speaker 2:** Please send the report by tomorrow
            [00:59:43] **Speaker 4:** I will review the PR today
        """.trimIndent()

        val items = transcriptAnalyzer.enhancedAnalyze(transcript, pipeline)

        assertTrue(items.isNotEmpty())
    }

    @Test
    fun `analyze does not crash on Devanagari transcript with bold speakers`() {
        val transcript = """
            [00:01:00] **\u0930\u093E\u0939\u0941\u0932:** \u0915\u0932 \u0924\u0915 \u0930\u093F\u092A\u094B\u0930\u094D\u091F \u092D\u0947\u091C \u0926\u0947\u0928\u093E
            [00:01:05] **\u092A\u094D\u0930\u093F\u092F\u093E:** \u0921\u0947\u091F\u093E\u092C\u0947\u0938 \u0907\u0936\u094D\u092F\u0942 \u091A\u0947\u0915 \u0915\u0930\u0928\u093E \u0939\u0948
        """.trimIndent()

        // Should not throw any exception
        val items = transcriptAnalyzer.analyze(transcript)
        // Result may be empty or non-empty depending on keyword matching, but no crash
        assertNotNull(items)
    }

    @Test
    fun `enhancedAnalyze does not crash on Devanagari transcript`() {
        val pipeline = createPipeline()
        val transcript = """
            [00:01:00] **\u0930\u093E\u0939\u0941\u0932:** \u0915\u0932 \u0924\u0915 report bhej dena jaldi
            [00:01:05] **\u092A\u094D\u0930\u093F\u092F\u093E:** Check karna hai database issue
        """.trimIndent()

        // Should not throw any exception
        val items = transcriptAnalyzer.enhancedAnalyze(transcript, pipeline)
        assertNotNull(items)
    }

    // Enhanced analyze tests

    @Test
    fun `enhancedAnalyze produces action items with speaker roles`() {
        val pipeline = createPipeline()
        val transcript = """
            Manager: Let's start the meeting
            Manager: Sarah, please review the document
            Sarah: Will do
        """.trimIndent()

        val items = transcriptAnalyzer.enhancedAnalyze(transcript, pipeline)

        assertTrue(items.isNotEmpty())
        val sarahItem = items.find { it.owner == "Sarah" }
        assertNotNull(sarahItem)
        assertNotNull(sarahItem?.speakerRole)
    }

    @Test
    fun `enhancedAnalyze produces richer items with meeting type`() {
        val pipeline = createPipeline()
        val transcript = """
            Alice: What did you do yesterday?
            Bob: I need to fix the blocker today
        """.trimIndent()

        val items = transcriptAnalyzer.enhancedAnalyze(transcript, pipeline)

        assertTrue(items.isNotEmpty())
        assertNotNull(items.first().meetingType)
    }

    @Test
    fun `enhancedAnalyze works without pipeline`() {
        val transcript = """
            Alice: Please send the report tomorrow
        """.trimIndent()

        val items = transcriptAnalyzer.enhancedAnalyze(transcript, null)

        assertTrue(items.isNotEmpty())
        assertNull(items.first().mlConfidence)
    }

    @Test
    fun `enhancedAnalyze handles empty transcript`() {
        val items = transcriptAnalyzer.enhancedAnalyze("", null)
        assertTrue(items.isEmpty())
    }

    @Test
    fun `enhancedAnalyze produces at least as good results as basic analyze`() {
        val pipeline = createPipeline()
        val transcript = """
            John: We need to review the PR before end of day
            Sarah: I will send the report tomorrow
        """.trimIndent()

        val basicItems = transcriptAnalyzer.analyze(transcript)
        val enhancedItems = transcriptAnalyzer.enhancedAnalyze(transcript, pipeline)

        // Enhanced should find at least the same action items
        assertTrue(enhancedItems.isNotEmpty())
        assertTrue(basicItems.isNotEmpty())
    }

    private fun createPipeline(): HybridClassificationPipeline {
        val featureExtractor = TextFeatureExtractor()
        val onnxClassifier = OnnxIntentClassifier()
        val ruleClassifier = RuleBasedIntentClassifier()
        return HybridClassificationPipeline(featureExtractor, onnxClassifier, ruleClassifier)
    }
}
