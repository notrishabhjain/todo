package com.procrastinationkiller.domain.engine.prioritization

import com.procrastinationkiller.domain.model.TaskPriority
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeadlineProximityEscalatorTest {

    private lateinit var escalator: DeadlineProximityEscalator
    private val now = 1700000000000L

    @BeforeEach
    fun setup() {
        escalator = DeadlineProximityEscalator()
    }

    @Test
    fun `task with 1 hour to deadline escalates to CRITICAL`() {
        val deadline = now + 30 * 60 * 1000 // 30 minutes

        val result = escalator.evaluate(deadline, TaskPriority.MEDIUM, now)

        assertNotNull(result)
        assertEquals(TaskPriority.CRITICAL, result!!.escalatedPriority)
        assertEquals("DEADLINE_WITHIN_1_HOUR", result.reason)
    }

    @Test
    fun `task with 3 hours to deadline escalates to HIGH`() {
        val deadline = now + 3 * 60 * 60 * 1000 // 3 hours

        val result = escalator.evaluate(deadline, TaskPriority.MEDIUM, now)

        assertNotNull(result)
        assertEquals(TaskPriority.HIGH, result!!.escalatedPriority)
        assertEquals("DEADLINE_WITHIN_4_HOURS", result.reason)
    }

    @Test
    fun `task with 12 hours to deadline escalates one level`() {
        val deadline = now + 12 * 60 * 60 * 1000 // 12 hours

        val result = escalator.evaluate(deadline, TaskPriority.LOW, now)

        assertNotNull(result)
        assertEquals(TaskPriority.MEDIUM, result!!.escalatedPriority)
        assertEquals("DEADLINE_WITHIN_24_HOURS", result.reason)
    }

    @Test
    fun `task with 12 hours and HIGH priority escalates to CRITICAL`() {
        val deadline = now + 12 * 60 * 60 * 1000 // 12 hours

        val result = escalator.evaluate(deadline, TaskPriority.HIGH, now)

        assertNotNull(result)
        assertEquals(TaskPriority.CRITICAL, result!!.escalatedPriority)
    }

    @Test
    fun `task with no deadline returns null`() {
        val result = escalator.evaluate(null, TaskPriority.MEDIUM, now)

        assertNull(result)
    }

    @Test
    fun `task with deadline more than 24 hours away returns null`() {
        val deadline = now + 48 * 60 * 60 * 1000 // 48 hours

        val result = escalator.evaluate(deadline, TaskPriority.MEDIUM, now)

        assertNull(result)
    }

    @Test
    fun `past deadline escalates to CRITICAL`() {
        val deadline = now - 60 * 60 * 1000 // 1 hour ago

        val result = escalator.evaluate(deadline, TaskPriority.LOW, now)

        assertNotNull(result)
        assertEquals(TaskPriority.CRITICAL, result!!.escalatedPriority)
        assertEquals("DEADLINE_PASSED", result.reason)
        assertEquals(1.0f, result.urgencyMultiplier)
    }

    @Test
    fun `urgency multiplier is higher for closer deadlines`() {
        val closeDeadline = now + 45 * 60 * 1000 // 45 minutes
        val farDeadline = now + 20 * 60 * 60 * 1000 // 20 hours

        val closeResult = escalator.evaluate(closeDeadline, TaskPriority.MEDIUM, now)
        val farResult = escalator.evaluate(farDeadline, TaskPriority.MEDIUM, now)

        assertNotNull(closeResult)
        assertNotNull(farResult)
        assertTrue(closeResult!!.urgencyMultiplier > farResult!!.urgencyMultiplier)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.jupiter.api.Assertions.assertTrue(condition)
    }
}
