package com.procrastinationkiller.domain.engine

import com.procrastinationkiller.domain.model.ReminderMode
import javax.inject.Inject
import javax.inject.Singleton

enum class ReminderFrequency(val displayName: String, val intervalMinutes: Long) {
    EVERY_15_MIN("Every 15 minutes", 15),
    EVERY_30_MIN("Every 30 minutes", 30),
    HOURLY("Hourly", 60),
    MORNING_EVENING("Morning & Evening only", 0)
}

@Singleton
class ReminderScheduler @Inject constructor() {

    fun getFrequencyForMode(mode: ReminderMode): ReminderFrequency {
        return when (mode) {
            ReminderMode.NUCLEAR -> ReminderFrequency.EVERY_15_MIN
            ReminderMode.AGGRESSIVE -> ReminderFrequency.EVERY_15_MIN
            ReminderMode.NORMAL -> ReminderFrequency.EVERY_30_MIN
            ReminderMode.GENTLE -> ReminderFrequency.MORNING_EVENING
        }
    }

    fun shouldShowReminder(mode: ReminderMode, hourOfDay: Int): Boolean {
        return when (mode) {
            ReminderMode.GENTLE -> hourOfDay == 8 || hourOfDay == 20
            ReminderMode.NORMAL -> hourOfDay in 7..22
            ReminderMode.AGGRESSIVE -> hourOfDay in 6..23
            ReminderMode.NUCLEAR -> true
        }
    }

    fun getWorkManagerIntervalMinutes(frequency: ReminderFrequency): Long {
        return when (frequency) {
            ReminderFrequency.EVERY_15_MIN -> 15L
            ReminderFrequency.EVERY_30_MIN -> 30L
            ReminderFrequency.HOURLY -> 60L
            ReminderFrequency.MORNING_EVENING -> 360L // Check every 6 hours, filter in worker
        }
    }

    fun shouldEscalateLanguage(mode: ReminderMode, consecutiveIgnoredCount: Int): Boolean {
        return when (mode) {
            ReminderMode.AGGRESSIVE -> consecutiveIgnoredCount >= 2
            ReminderMode.NUCLEAR -> consecutiveIgnoredCount >= 1
            else -> false
        }
    }

    fun isPersistentNotificationRequired(mode: ReminderMode): Boolean {
        return when (mode) {
            ReminderMode.NUCLEAR -> true
            ReminderMode.AGGRESSIVE -> true
            ReminderMode.NORMAL -> false
            ReminderMode.GENTLE -> false
        }
    }
}
