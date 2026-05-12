package com.procrastinationkiller.domain.engine.prioritization

import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeOfDayAnalyzer @Inject constructor() {

    companion object {
        // Early morning (6-8) and late evening (21-23) messages tend to be more urgent
        private val HIGH_URGENCY_HOURS = setOf(6, 7, 21, 22, 23)
        // Work hours messages are standard priority
        private val NORMAL_HOURS = (9..17).toSet()
        // Off-hours (late night) messages often indicate urgency
        private val LATE_NIGHT_HOURS = setOf(0, 1, 2, 3, 4, 5)
    }

    fun analyze(
        timestampMs: Long = System.currentTimeMillis()
    ): TimeOfDayResult {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampMs
        }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val urgencyBoost: Float
        val factor: String?

        when {
            hour in LATE_NIGHT_HOURS -> {
                urgencyBoost = 0.2f
                factor = "LATE_NIGHT_MESSAGE"
            }
            hour in HIGH_URGENCY_HOURS -> {
                urgencyBoost = 0.1f
                factor = "OFF_HOURS_MESSAGE"
            }
            hour in NORMAL_HOURS -> {
                urgencyBoost = 0f
                factor = null
            }
            else -> {
                urgencyBoost = 0f
                factor = null
            }
        }

        return TimeOfDayResult(
            urgencyBoost = urgencyBoost,
            factor = factor,
            hour = hour
        )
    }
}

data class TimeOfDayResult(
    val urgencyBoost: Float,
    val factor: String?,
    val hour: Int
)
