package com.procrastinationkiller.domain.engine.ml

import com.procrastinationkiller.domain.engine.KeywordAnalysis
import com.procrastinationkiller.domain.engine.KeywordEngine
import com.procrastinationkiller.domain.engine.KeywordMatch
import com.procrastinationkiller.domain.model.TaskPriority
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HybridClassificationPipelineTest {

    private lateinit var pipeline: HybridClassificationPipeline
    private lateinit var keywordEngine: KeywordEngine
    private lateinit var featureExtractor: TextFeatureExtractor
    private lateinit var onnxClassifier: OnnxIntentClassifier
    private lateinit var ruleBasedClassifier: RuleBasedIntentClassifier

    @BeforeEach
    fun setup() {
        featureExtractor = TextFeatureExtractor()
        onnxClassifier = OnnxIntentClassifier()
        ruleBasedClassifier = RuleBasedIntentClassifier()
        keywordEngine = KeywordEngine()

        pipeline = HybridClassificationPipeline(
            textFeatureExtractor = featureExtractor,
            onnxIntentClassifier = onnxClassifier,
            ruleBasedIntentClassifier = ruleBasedClassifier
        )
    }

    @Test
    fun `falls back to rules when ONNX model is unavailable`() {
        val text = "Send the report by tomorrow"
        val analysis = keywordEngine.analyze(text)
        val result = pipeline.classify(text, analysis)

        // Should use RULES source since ONNX model is not available
        assertEquals(ClassificationSource.RULES, result.source)
    }

    @Test
    fun `classifies actionable text correctly`() {
        val text = "Please send the report urgently"
        val analysis = keywordEngine.analyze(text)
        val result = pipeline.classify(text, analysis)

        assertTrue(result.isActionable)
        assertTrue(result.confidence > 0f)
    }

    @Test
    fun `classifies non-actionable text correctly`() {
        val text = "Good morning everyone"
        val analysis = keywordEngine.analyze(text)
        val result = pipeline.classify(text, analysis)

        // Keyword analysis says not actionable, rules say not actionable
        assertFalse(result.isActionable)
    }

    @Test
    fun `combines keyword and rule-based results for actionability`() {
        // Text with action keywords detected by KeywordEngine
        val text = "Check the logs when you get a chance"
        val analysis = keywordEngine.analyze(text)
        val result = pipeline.classify(text, analysis)

        // KeywordEngine finds "check" as action keyword, so isActionable should be true
        assertTrue(result.isActionable)
    }

    @Test
    fun `provides priority from classification`() {
        val text = "Urgent! Send the report ASAP"
        val analysis = keywordEngine.analyze(text)
        val result = pipeline.classify(text, analysis)

        // Should have a priority assigned
        assertTrue(result.finalPriority != null)
        assertTrue(
            result.finalPriority == TaskPriority.HIGH ||
                result.finalPriority == TaskPriority.CRITICAL
        )
    }

    @Test
    fun `confidence is within valid range`() {
        val texts = listOf(
            "Send the report",
            "Good morning!",
            "Urgent deploy to production ASAP",
            "What time is the meeting?"
        )

        for (text in texts) {
            val analysis = keywordEngine.analyze(text)
            val result = pipeline.classify(text, analysis)
            assertTrue(result.confidence in 0f..1f, "Confidence out of range for: $text")
        }
    }

    @Test
    fun `handles empty text`() {
        val text = ""
        val analysis = keywordEngine.analyze(text)
        val result = pipeline.classify(text, analysis)

        assertFalse(result.isActionable)
    }

    @Test
    fun `returns intent type`() {
        val text = "Please review the PR and deploy to staging"
        val analysis = keywordEngine.analyze(text)
        val result = pipeline.classify(text, analysis)

        // Should identify as task request
        assertTrue(result.intent == IntentType.TASK_REQUEST || result.intent == IntentType.MEETING)
    }

    @Test
    fun `question text classified with appropriate intent`() {
        val text = "What is the status of the deployment?"
        val analysis = keywordEngine.analyze(text)
        val result = pipeline.classify(text, analysis)

        // Questions without strong action verbs should not be task requests
        assertTrue(
            result.intent == IntentType.QUESTION ||
                result.intent == IntentType.INFORMATION
        )
    }

    @Test
    fun `integration with KeywordEngine produces consistent results`() {
        val text = "Bhai kal tak proposal bhej dena"
        val analysis = keywordEngine.analyze(text)
        val result = pipeline.classify(text, analysis)

        // KeywordEngine should find this actionable, pipeline should agree
        assertTrue(analysis.isActionable)
        assertTrue(result.isActionable)
    }
}
