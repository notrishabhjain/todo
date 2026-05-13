package com.procrastinationkiller.domain.engine.semantic

import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImplicitDeadlineResolver @Inject constructor() {

    companion object {
        private val DEADLINE_PATTERNS = listOf(
            "before end of day" to ::resolveEndOfDay,
            "by end of day" to ::resolveEndOfDay,
            "end of day" to ::resolveEndOfDay,
            "before eod" to ::resolveEndOfDay,
            "first thing tomorrow" to ::resolveFirstThingTomorrow,
            "first thing in the morning" to ::resolveFirstThingTomorrow,
            "this weekend" to ::resolveThisWeekend,
            "before the meeting" to ::resolveNextBusinessHour,
            "before i leave" to ::resolveBeforeLeave,
            "before leaving" to ::resolveBeforeLeave,
            "asap" to ::resolveAsap,
            "as soon as possible" to ::resolveAsap,
            "right away" to ::resolveAsap,
            "aaj shaam tak" to ::resolveEndOfDay,
            "aaj tak" to ::resolveEndOfDay,
            "kal subah" to ::resolveFirstThingTomorrow
        )

        private fun resolveEndOfDay(now: Calendar): Long {
            val cal = now.clone() as Calendar
            cal.set(Calendar.HOUR_OF_DAY, 18)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            // If already past 6PM, set to tomorrow 6PM
            if (cal.timeInMillis <= now.timeInMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        private fun resolveFirstThingTomorrow(now: Calendar): Long {
            val cal = now.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        private fun resolveThisWeekend(now: Calendar): Long {
            val cal = now.clone() as Calendar
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val daysUntilSaturday = if (dayOfWeek == Calendar.SATURDAY) {
                0
            } else if (dayOfWeek == Calendar.SUNDAY) {
                6
            } else {
                Calendar.SATURDAY - dayOfWeek
            }
            cal.add(Calendar.DAY_OF_YEAR, daysUntilSaturday)
            cal.set(Calendar.HOUR_OF_DAY, 10)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        private fun resolveNextBusinessHour(now: Calendar): Long {
            val cal = now.clone() as Calendar
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            if (currentHour < 17) {
                // Next hour within business day
                cal.add(Calendar.HOUR_OF_DAY, 1)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            } else {
                // Next business day 9 AM
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        private fun resolveBeforeLeave(now: Calendar): Long {
            return resolveEndOfDay(now)
        }

        private fun resolveAsap(now: Calendar): Long {
            val cal = now.clone() as Calendar
            cal.add(Calendar.HOUR_OF_DAY, 1)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }

    fun resolve(text: String): ImplicitDeadline? {
        return resolveWithCalendar(text, Calendar.getInstance())
    }

    internal fun resolveWithCalendar(text: String, now: Calendar): ImplicitDeadline? {
        val lowerText = text.lowercase()

        for ((pattern, resolver) in DEADLINE_PATTERNS) {
            if (lowerText.contains(pattern)) {
                val timestamp = resolver(now)
                return ImplicitDeadline(
                    resolvedTimestamp = timestamp,
                    sourcePhrase = pattern
                )
            }
        }

        return null
    }
}
