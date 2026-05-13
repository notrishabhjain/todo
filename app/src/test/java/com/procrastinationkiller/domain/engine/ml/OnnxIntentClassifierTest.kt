package com.procrastinationkiller.domain.engine.ml

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Tests for OnnxIntentClassifier behavior when no model is available.
 * Uses the no-arg test constructor which creates a classifier with modelAvailable = false.
 */
class OnnxIntentClassifierTest {

    @Test
    fun `returns null when model is not available`() {
        val classifier = OnnxIntentClassifier()
        val features = FloatArray(TextFeatureExtractor.FEATURE_COUNT) { 0f }
        val result = classifier.classify(features)
        assertNull(result)
    }

    @Test
    fun `returns null for any feature input when model unavailable`() {
        val classifier = OnnxIntentClassifier()

        val featureSets = listOf(
            FloatArray(TextFeatureExtractor.FEATURE_COUNT) { 0f },
            FloatArray(TextFeatureExtractor.FEATURE_COUNT) { 1f },
            FloatArray(TextFeatureExtractor.FEATURE_COUNT) { 0.5f },
            FloatArray(TextFeatureExtractor.FEATURE_COUNT) { index -> index.toFloat() / TextFeatureExtractor.FEATURE_COUNT }
        )

        for (features in featureSets) {
            assertNull(classifier.classify(features), "Expected null for features: ${features.toList()}")
        }
    }

    @Test
    fun `model version is defined`() {
        assert(OnnxIntentClassifier.MODEL_VERSION >= 1)
    }
}
