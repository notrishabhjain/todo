package com.procrastinationkiller.domain.engine.ml

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TextFeatureExtractorTest {

    private lateinit var extractor: TextFeatureExtractor

    @BeforeEach
    fun setup() {
        extractor = TextFeatureExtractor()
    }

    @Test
    fun `produces correct feature count`() {
        val features = extractor.extract("Send the report by tomorrow")
        assertEquals(TextFeatureExtractor.FEATURE_COUNT, features.size)
    }

    @Test
    fun `all features are between 0 and 1`() {
        val features = extractor.extract("Urgent! Please send the report ASAP by tomorrow!!!")
        for (i in features.indices) {
            assertTrue(features[i] in 0f..1f, "Feature $i out of range: ${features[i]}")
        }
    }

    @Test
    fun `detects action verbs`() {
        val actionText = extractor.extract("send submit review check deploy")
        val noActionText = extractor.extract("hello good morning everyone today")

        // Action verb density (feature index 1) should be higher for action text
        assertTrue(actionText[1] > noActionText[1])
    }

    @Test
    fun `detects urgency keywords`() {
        val urgentText = extractor.extract("urgent asap critical important deadline")
        val calmText = extractor.extract("whenever you get time please look at this thing")

        // Urgency density (feature index 2) should be higher for urgent text
        assertTrue(urgentText[2] > calmText[2])
    }

    @Test
    fun `detects question marks`() {
        val questionText = extractor.extract("Can you send the report?")
        val statementText = extractor.extract("Send the report now")

        // Question presence (feature index 3) should be 1.0 for question
        assertEquals(1.0f, questionText[3])
        assertEquals(0.0f, statementText[3])
    }

    @Test
    fun `counts exclamation marks`() {
        val exclamText = extractor.extract("Urgent!! Do it now!!!")
        val noExclamText = extractor.extract("Please send the report")

        // Exclamation count (feature index 4) should be higher
        assertTrue(exclamText[4] > noExclamText[4])
    }

    @Test
    fun `detects time references`() {
        val timeText = extractor.extract("Send it by tomorrow morning")
        val noTimeText = extractor.extract("Send the file please")

        // Time reference (feature index 5) should be higher for time text
        assertTrue(timeText[5] > noTimeText[5])
    }

    @Test
    fun `handles empty string gracefully`() {
        val features = extractor.extract("")
        assertEquals(TextFeatureExtractor.FEATURE_COUNT, features.size)
        // All features should be 0 for empty text
        for (i in features.indices) {
            assertEquals(0f, features[i], "Feature $i should be 0 for empty text")
        }
    }

    @Test
    fun `handles very long text`() {
        val longText = "send ".repeat(200)
        val features = extractor.extract(longText)
        assertEquals(TextFeatureExtractor.FEATURE_COUNT, features.size)
        // Word count should be capped at 1.0
        assertEquals(1.0f, features[0])
    }

    @Test
    fun `handles non-English text`() {
        val hindiText = "karna jaldi bhejo abhi zaruri"
        val features = extractor.extract(hindiText)
        assertEquals(TextFeatureExtractor.FEATURE_COUNT, features.size)
        // Should detect Hinglish action verbs and urgency
        assertTrue(features[1] > 0f, "Should detect Hinglish action verbs")
        assertTrue(features[2] > 0f, "Should detect Hinglish urgency keywords")
    }

    @Test
    fun `calculates capitalization ratio`() {
        val allCapsText = extractor.extract("SEND THE REPORT NOW")
        val lowerText = extractor.extract("send the report now")

        // Capitalization ratio (feature index 7) should be higher for all caps
        assertTrue(allCapsText[7] > lowerText[7])
    }

    @Test
    fun `consistent output for same input`() {
        val text = "Please review the PR by tomorrow"
        val features1 = extractor.extract(text)
        val features2 = extractor.extract(text)

        for (i in features1.indices) {
            assertEquals(features1[i], features2[i], "Feature $i should be consistent")
        }
    }
}
