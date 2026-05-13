package com.procrastinationkiller.domain.engine.semantic

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NegationDetectorTest {

    private lateinit var detector: NegationDetector

    @BeforeEach
    fun setup() {
        detector = NegationDetector()
    }

    @Test
    fun `don't worry about it is negated`() {
        val result = detector.detect("don't worry about it")
        assertTrue(result.isNegated)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `do worry about it is NOT negated`() {
        val result = detector.detect("do worry about it")
        assertFalse(result.isNegated)
    }

    @Test
    fun `no need to send is negated`() {
        val result = detector.detect("no need to send the report")
        assertTrue(result.isNegated)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `please send is NOT negated`() {
        val result = detector.detect("please send the report")
        assertFalse(result.isNegated)
    }

    @Test
    fun `rehne do is negated`() {
        val result = detector.detect("rehne do bhai")
        assertTrue(result.isNegated)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `mat karo is negated`() {
        val result = detector.detect("mat karo yeh kaam")
        assertTrue(result.isNegated)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `karo alone is NOT negated`() {
        val result = detector.detect("karo yeh kaam")
        assertFalse(result.isNegated)
    }

    @Test
    fun `nevermind is negated`() {
        val result = detector.detect("nevermind, forget it")
        assertTrue(result.isNegated)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `ignore that is negated`() {
        val result = detector.detect("ignore that previous message")
        assertTrue(result.isNegated)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `nahi chahiye is negated`() {
        val result = detector.detect("nahi chahiye woh file")
        assertTrue(result.isNegated)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `zaroorat nahi is negated`() {
        val result = detector.detect("zaroorat nahi hai")
        assertTrue(result.isNegated)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `positive action message is NOT negated`() {
        val result = detector.detect("Please send the report by tomorrow")
        assertFalse(result.isNegated)
    }
}
