package com.procrastinationkiller.domain.engine.semantic

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QuestionVsRequestClassifierTest {

    private lateinit var classifier: QuestionVsRequestClassifier

    @BeforeEach
    fun setup() {
        classifier = QuestionVsRequestClassifier()
    }

    @Test
    fun `Did you send it is a question`() {
        val result = classifier.classify("Did you send it?")
        assertTrue(result.isQuestion)
        assertFalse(result.isPoliteRequest)
    }

    @Test
    fun `Please send it is a request`() {
        val result = classifier.classify("Please send it")
        assertFalse(result.isQuestion)
        assertTrue(result.isPoliteRequest)
    }

    @Test
    fun `Can you send it by tomorrow is a polite request and actionable`() {
        val result = classifier.classify("Can you send it by tomorrow?")
        assertFalse(result.isQuestion)
        assertTrue(result.isPoliteRequest)
    }

    @Test
    fun `Have you finished the report is a question`() {
        val result = classifier.classify("Have you finished the report?")
        assertTrue(result.isQuestion)
        assertFalse(result.isPoliteRequest)
    }

    @Test
    fun `Finish the report is a request`() {
        val result = classifier.classify("Finish the report")
        assertFalse(result.isQuestion)
        assertFalse(result.isPoliteRequest)
    }

    @Test
    fun `Kya tumne bheja is a question`() {
        val result = classifier.classify("Kya tumne bheja?")
        assertTrue(result.isQuestion)
        assertFalse(result.isPoliteRequest)
    }

    @Test
    fun `Could you review the code is a polite request`() {
        val result = classifier.classify("Could you review the code?")
        assertFalse(result.isQuestion)
        assertTrue(result.isPoliteRequest)
    }

    @Test
    fun `Is the meeting today is a question`() {
        val result = classifier.classify("Is the meeting today?")
        assertTrue(result.isQuestion)
        assertFalse(result.isPoliteRequest)
    }

    @Test
    fun `Send the report is a request (imperative)`() {
        val result = classifier.classify("Send the report to the client")
        assertFalse(result.isQuestion)
    }

    @Test
    fun `Have you finished is status check`() {
        val result = classifier.classify("Have you finished?")
        assertTrue(result.isQuestion)
        assertFalse(result.isPoliteRequest)
    }
}
