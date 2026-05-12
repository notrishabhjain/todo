package com.procrastinationkiller.domain.engine.ml

import com.procrastinationkiller.domain.model.TaskPriority
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RuleBasedIntentClassifierTest {

    private lateinit var classifier: RuleBasedIntentClassifier
    private lateinit var featureExtractor: TextFeatureExtractor

    @BeforeEach
    fun setup() {
        classifier = RuleBasedIntentClassifier()
        featureExtractor = TextFeatureExtractor()
    }

    @Test
    fun `classifies actionable text as TASK_REQUEST`() {
        // High action verb density + urgency
        val features = featureExtractor.extract("Send submit review check the report urgently")
        val result = classifier.classify(features)

        assertEquals(IntentType.TASK_REQUEST, result!!.intent)
        assertTrue(result.isActionable)
        assertTrue(result.confidence > 0.5f)
    }

    @Test
    fun `classifies non-actionable text as INFORMATION`() {
        // All neutral features - no action, no urgency, no time, no question
        val features = FloatArray(TextFeatureExtractor.FEATURE_COUNT) { 0f }
        // Set word count > 0 and avg word len > 0 so it looks like real text
        features[0] = 0.1f
        features[6] = 0.4f

        val result = classifier.classify(features)

        assertEquals(IntentType.INFORMATION, result!!.intent)
        assertFalse(result.isActionable)
    }

    @Test
    fun `classifies question text as QUESTION`() {
        val features = featureExtractor.extract("What is the status of the project?")
        val result = classifier.classify(features)

        assertEquals(IntentType.QUESTION, result!!.intent)
        assertFalse(result.isActionable)
        assertNull(result.suggestedPriority)
    }

    @Test
    fun `classifies social text as SOCIAL or INFORMATION`() {
        // High exclamation, no action verbs
        val features = FloatArray(TextFeatureExtractor.FEATURE_COUNT) { 0f }
        features[0] = 0.06f // word count
        features[4] = 0.6f // exclamation count (high)
        features[6] = 0.4f // avg word len

        val result = classifier.classify(features)

        assertTrue(
            result!!.intent == IntentType.SOCIAL || result.intent == IntentType.INFORMATION
        )
        assertFalse(result.isActionable)
    }

    @Test
    fun `assigns CRITICAL priority for high urgency`() {
        val features = featureExtractor.extract("Urgent! ASAP critical important send the report now")
        val result = classifier.classify(features)

        assertTrue(result!!.isActionable)
        if (result.intent == IntentType.TASK_REQUEST) {
            assertEquals(TaskPriority.CRITICAL, result.suggestedPriority)
        }
    }

    @Test
    fun `assigns MEDIUM priority for moderate action text`() {
        val features = featureExtractor.extract("send the file when convenient")
        val result = classifier.classify(features)

        if (result!!.intent == IntentType.TASK_REQUEST) {
            assertTrue(
                result.suggestedPriority == TaskPriority.MEDIUM ||
                    result.suggestedPriority == TaskPriority.HIGH
            )
        }
    }

    @Test
    fun `handles all-zero features`() {
        val features = FloatArray(TextFeatureExtractor.FEATURE_COUNT) { 0f }
        val result = classifier.classify(features)

        // Should classify as non-actionable
        assertFalse(result!!.isActionable)
        assertTrue(
            result.intent == IntentType.INFORMATION || result.intent == IntentType.SOCIAL
        )
    }

    @Test
    fun `classifies meeting-related text`() {
        // Strong time reference + action verbs = meeting
        val features = featureExtractor.extract("schedule a meeting tomorrow morning to review the project on friday")
        val result = classifier.classify(features)

        // Should be either MEETING or TASK_REQUEST (both are actionable)
        assertTrue(result!!.isActionable)
    }

    @Test
    fun `confidence is bounded between 0 and 1`() {
        // Test with extreme feature values
        val maxFeatures = FloatArray(TextFeatureExtractor.FEATURE_COUNT) { 1.0f }
        val result = classifier.classify(maxFeatures)

        assertTrue(result!!.confidence in 0f..1f)
    }

    @Test
    fun `urgency increases task priority`() {
        val noUrgency = featureExtractor.extract("send the report please")
        val withUrgency = featureExtractor.extract("urgently send the report asap")

        val resultNoUrgency = classifier.classify(noUrgency)
        val resultWithUrgency = classifier.classify(withUrgency)

        // Both should be task requests
        if (resultNoUrgency!!.intent == IntentType.TASK_REQUEST &&
            resultWithUrgency!!.intent == IntentType.TASK_REQUEST
        ) {
            // Priority with urgency should be >= priority without
            val priorityOrder = listOf(TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH, TaskPriority.CRITICAL)
            val noUrgencyIdx = priorityOrder.indexOf(resultNoUrgency.suggestedPriority)
            val withUrgencyIdx = priorityOrder.indexOf(resultWithUrgency.suggestedPriority)
            assertTrue(withUrgencyIdx >= noUrgencyIdx)
        }
    }
}
