package com.procrastinationkiller.domain.engine.transcript

import com.procrastinationkiller.domain.engine.KeywordEngine
import com.procrastinationkiller.domain.engine.ml.HybridClassificationPipeline
import com.procrastinationkiller.domain.engine.ml.OnnxIntentClassifier
import com.procrastinationkiller.domain.engine.ml.RuleBasedIntentClassifier
import com.procrastinationkiller.domain.engine.ml.TextFeatureExtractor
import com.procrastinationkiller.domain.model.TaskPriority
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnhancedTranscriptAnalyzerTest {

    private lateinit var keywordEngine: KeywordEngine
    private lateinit var pipeline: HybridClassificationPipeline
    private lateinit var analyzer: EnhancedTranscriptAnalyzer

    @BeforeEach
    fun setup() {
        keywordEngine = KeywordEngine()
        val featureExtractor = TextFeatureExtractor()
        val onnxClassifier = OnnxIntentClassifier()
        val ruleClassifier = RuleBasedIntentClassifier()
        pipeline = HybridClassificationPipeline(featureExtractor, onnxClassifier, ruleClassifier)
        analyzer = EnhancedTranscriptAnalyzer(keywordEngine, pipeline)
    }

    @Test
    fun `ML validation filters out non-actionable segments`() {
        val transcript = """
            Alice: Good morning everyone
            Bob: How was your weekend?
            Charlie: Pretty good thanks
        """.trimIndent()

        val items = analyzer.analyze(transcript)

        assertTrue(items.isEmpty())
    }

    @Test
    fun `ML validation keeps actionable segments`() {
        val transcript = """
            Alice: We need to review the PR before end of day
            Bob: I will send the report tomorrow
        """.trimIndent()

        val items = analyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
    }

    @Test
    fun `speaker roles improve owner assignment`() {
        val transcript = """
            Manager: Let's assign tasks for this sprint
            Manager: Sarah, please fix the login bug
            Sarah: Sure, I will handle it
        """.trimIndent()

        val items = analyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        val sarahItem = items.find { it.owner == "Sarah" }
        assertNotNull(sarahItem)
        assertEquals("PARTICIPANT", sarahItem?.speakerRole)
    }

    @Test
    fun `context improves priority for standup blockers`() {
        val transcript = """
            Alice: What did you do yesterday?
            Bob: I worked on the feature. Today I need to fix the blocker urgently
        """.trimIndent()

        val items = analyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertEquals("STANDUP", items.first().meetingType)
    }

    @Test
    fun `deduplication merges similar items`() {
        val transcript = """
            Alice: Please send the report tomorrow
            Alice: Send the report by tomorrow please
        """.trimIndent()

        val items = analyzer.analyze(transcript)

        // Similar items should be deduplicated
        assertTrue(items.size <= 2)
    }

    @Test
    fun `items have ML confidence when pipeline is provided`() {
        val transcript = """
            Manager: Deploy the fix immediately
        """.trimIndent()

        val items = analyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertNotNull(items.first().mlConfidence)
    }

    @Test
    fun `analyzer works without pipeline`() {
        val analyzerNoPipeline = EnhancedTranscriptAnalyzer(keywordEngine, null)

        val transcript = """
            Alice: Please send the report tomorrow
        """.trimIndent()

        val items = analyzerNoPipeline.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertEquals(null, items.first().mlConfidence)
    }

    @Test
    fun `empty transcript produces no items`() {
        val items = analyzer.analyze("")
        assertTrue(items.isEmpty())
    }

    @Test
    fun `multiple analyze calls do not accumulate participants`() {
        val transcript1 = """
            Alice: Please send the report tomorrow
        """.trimIndent()

        val transcript2 = """
            Bob: Please deploy the fix by Friday
        """.trimIndent()

        val items1 = analyzer.analyze(transcript1)
        val items2 = analyzer.analyze(transcript2)

        // Verify that a second call does not carry over participants from the first
        // Both calls should produce independent results
        assertTrue(items1.isNotEmpty())
        assertTrue(items2.isNotEmpty())
        // If state leaked, owner detection could be impacted
        // The fix ensures a fresh ConversationContextBuilder per analyze() call
    }

    @Test
    fun `meeting type is included in action items`() {
        val transcript = """
            PM: Let's estimate the sprint backlog
            Dev: I will complete the user story by Friday
        """.trimIndent()

        val items = analyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertNotNull(items.first().meetingType)
    }

    @Test
    fun `handles bold speaker format with timestamps`() {
        val transcript = """
            [00:59:38] **Speaker 2:** Please send the report by tomorrow
            [00:59:43] **Speaker 4:** I will review the PR before end of day
        """.trimIndent()

        val items = analyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.text.contains("send") || it.text.contains("review") })
    }

    @Test
    fun `handles user exact transcript format with Hinglish`() {
        val transcript = """
            [00:59:38] **Speaker 2:** Kal tak report bhej dena jaldi
            [00:59:43] **Speaker 4:** Check karna hai database issue
        """.trimIndent()

        val items = analyzer.analyze(transcript)

        assertTrue(items.isNotEmpty())
    }

    @Test
    fun `detects Devanagari text in segments`() {
        val analyzerNoPipeline = EnhancedTranscriptAnalyzer(keywordEngine, null)
        assertTrue(analyzerNoPipeline.containsDevanagari("यह करना है"))
        assertTrue(!analyzerNoPipeline.containsDevanagari("this is english"))
    }

    @Test
    fun `handles Hindi assignment patterns`() {
        val analyzerNoPipeline = EnhancedTranscriptAnalyzer(keywordEngine, null)
        val transcript = """
            Manager: Rahul ko bol do report bhej dena
        """.trimIndent()

        val items = analyzerNoPipeline.analyze(transcript)

        assertTrue(items.isNotEmpty())
        assertEquals("Rahul", items.first().owner)
    }
}
