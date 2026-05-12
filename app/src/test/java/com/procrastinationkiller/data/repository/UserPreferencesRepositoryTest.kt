package com.procrastinationkiller.data.repository

import com.procrastinationkiller.domain.engine.ReminderFrequency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserPreferencesRepositoryTest {

    @Test
    fun `ReminderFrequency EVERY_15_MIN has correct interval`() {
        assertEquals(15L, ReminderFrequency.EVERY_15_MIN.intervalMinutes)
    }

    @Test
    fun `ReminderFrequency EVERY_30_MIN has correct interval`() {
        assertEquals(30L, ReminderFrequency.EVERY_30_MIN.intervalMinutes)
    }

    @Test
    fun `ReminderFrequency HOURLY has correct interval`() {
        assertEquals(60L, ReminderFrequency.HOURLY.intervalMinutes)
    }

    @Test
    fun `ReminderFrequency MORNING_EVENING has zero interval`() {
        assertEquals(0L, ReminderFrequency.MORNING_EVENING.intervalMinutes)
    }

    @Test
    fun `ReminderFrequency display names are human readable`() {
        assertEquals("Every 15 minutes", ReminderFrequency.EVERY_15_MIN.displayName)
        assertEquals("Every 30 minutes", ReminderFrequency.EVERY_30_MIN.displayName)
        assertEquals("Hourly", ReminderFrequency.HOURLY.displayName)
        assertEquals("Morning & Evening only", ReminderFrequency.MORNING_EVENING.displayName)
    }

    @Test
    fun `ReminderFrequency valueOf works correctly`() {
        assertEquals(ReminderFrequency.EVERY_15_MIN, ReminderFrequency.valueOf("EVERY_15_MIN"))
        assertEquals(ReminderFrequency.EVERY_30_MIN, ReminderFrequency.valueOf("EVERY_30_MIN"))
        assertEquals(ReminderFrequency.HOURLY, ReminderFrequency.valueOf("HOURLY"))
        assertEquals(ReminderFrequency.MORNING_EVENING, ReminderFrequency.valueOf("MORNING_EVENING"))
    }
}
