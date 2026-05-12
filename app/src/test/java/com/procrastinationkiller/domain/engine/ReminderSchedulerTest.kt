package com.procrastinationkiller.domain.engine

import com.procrastinationkiller.domain.model.ReminderMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReminderSchedulerTest {

    private lateinit var scheduler: ReminderScheduler

    @BeforeEach
    fun setup() {
        scheduler = ReminderScheduler()
    }

    @Test
    fun `aggressive mode returns 15 min frequency`() {
        val frequency = scheduler.getFrequencyForMode(ReminderMode.AGGRESSIVE)
        assertEquals(ReminderFrequency.EVERY_15_MIN, frequency)
    }

    @Test
    fun `nuclear mode returns 15 min frequency`() {
        val frequency = scheduler.getFrequencyForMode(ReminderMode.NUCLEAR)
        assertEquals(ReminderFrequency.EVERY_15_MIN, frequency)
    }

    @Test
    fun `normal mode returns 30 min frequency`() {
        val frequency = scheduler.getFrequencyForMode(ReminderMode.NORMAL)
        assertEquals(ReminderFrequency.EVERY_30_MIN, frequency)
    }

    @Test
    fun `gentle mode returns morning evening frequency`() {
        val frequency = scheduler.getFrequencyForMode(ReminderMode.GENTLE)
        assertEquals(ReminderFrequency.MORNING_EVENING, frequency)
    }

    @Test
    fun `gentle mode only shows at 8am and 8pm`() {
        assertTrue(scheduler.shouldShowReminder(ReminderMode.GENTLE, 8))
        assertTrue(scheduler.shouldShowReminder(ReminderMode.GENTLE, 20))
        assertFalse(scheduler.shouldShowReminder(ReminderMode.GENTLE, 12))
        assertFalse(scheduler.shouldShowReminder(ReminderMode.GENTLE, 3))
    }

    @Test
    fun `normal mode shows between 7am and 10pm`() {
        assertTrue(scheduler.shouldShowReminder(ReminderMode.NORMAL, 7))
        assertTrue(scheduler.shouldShowReminder(ReminderMode.NORMAL, 12))
        assertTrue(scheduler.shouldShowReminder(ReminderMode.NORMAL, 22))
        assertFalse(scheduler.shouldShowReminder(ReminderMode.NORMAL, 23))
        assertFalse(scheduler.shouldShowReminder(ReminderMode.NORMAL, 6))
    }

    @Test
    fun `aggressive mode shows between 6am and 11pm`() {
        assertTrue(scheduler.shouldShowReminder(ReminderMode.AGGRESSIVE, 6))
        assertTrue(scheduler.shouldShowReminder(ReminderMode.AGGRESSIVE, 23))
        assertFalse(scheduler.shouldShowReminder(ReminderMode.AGGRESSIVE, 5))
    }

    @Test
    fun `nuclear mode shows at all hours`() {
        assertTrue(scheduler.shouldShowReminder(ReminderMode.NUCLEAR, 0))
        assertTrue(scheduler.shouldShowReminder(ReminderMode.NUCLEAR, 3))
        assertTrue(scheduler.shouldShowReminder(ReminderMode.NUCLEAR, 12))
        assertTrue(scheduler.shouldShowReminder(ReminderMode.NUCLEAR, 23))
    }

    @Test
    fun `work manager interval for 15 min frequency`() {
        val interval = scheduler.getWorkManagerIntervalMinutes(ReminderFrequency.EVERY_15_MIN)
        assertEquals(15L, interval)
    }

    @Test
    fun `work manager interval for 30 min frequency`() {
        val interval = scheduler.getWorkManagerIntervalMinutes(ReminderFrequency.EVERY_30_MIN)
        assertEquals(30L, interval)
    }

    @Test
    fun `work manager interval for hourly frequency`() {
        val interval = scheduler.getWorkManagerIntervalMinutes(ReminderFrequency.HOURLY)
        assertEquals(60L, interval)
    }

    @Test
    fun `work manager interval for morning evening frequency`() {
        val interval = scheduler.getWorkManagerIntervalMinutes(ReminderFrequency.MORNING_EVENING)
        assertEquals(360L, interval)
    }

    @Test
    fun `escalate language for aggressive with 2 ignored`() {
        assertTrue(scheduler.shouldEscalateLanguage(ReminderMode.AGGRESSIVE, 2))
    }

    @Test
    fun `no escalation for aggressive with 1 ignored`() {
        assertFalse(scheduler.shouldEscalateLanguage(ReminderMode.AGGRESSIVE, 1))
    }

    @Test
    fun `escalate language for nuclear with 1 ignored`() {
        assertTrue(scheduler.shouldEscalateLanguage(ReminderMode.NUCLEAR, 1))
    }

    @Test
    fun `no escalation for normal mode`() {
        assertFalse(scheduler.shouldEscalateLanguage(ReminderMode.NORMAL, 5))
    }

    @Test
    fun `no escalation for gentle mode`() {
        assertFalse(scheduler.shouldEscalateLanguage(ReminderMode.GENTLE, 10))
    }

    @Test
    fun `persistent notification required for aggressive`() {
        assertTrue(scheduler.isPersistentNotificationRequired(ReminderMode.AGGRESSIVE))
    }

    @Test
    fun `persistent notification required for nuclear`() {
        assertTrue(scheduler.isPersistentNotificationRequired(ReminderMode.NUCLEAR))
    }

    @Test
    fun `persistent notification not required for normal`() {
        assertFalse(scheduler.isPersistentNotificationRequired(ReminderMode.NORMAL))
    }

    @Test
    fun `persistent notification not required for gentle`() {
        assertFalse(scheduler.isPersistentNotificationRequired(ReminderMode.GENTLE))
    }
}
