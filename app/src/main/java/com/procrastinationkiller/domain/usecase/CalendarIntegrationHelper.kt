package com.procrastinationkiller.domain.usecase

import android.content.Intent
import android.provider.CalendarContract
import com.procrastinationkiller.data.local.entity.TaskEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarIntegrationHelper @Inject constructor() {

    fun createCalendarIntent(task: TaskEntity): Intent {
        val startTime = task.deadline ?: (System.currentTimeMillis() + ONE_HOUR_MS)
        val endTime = startTime + ONE_HOUR_MS

        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, task.title)
            putExtra(CalendarContract.Events.DESCRIPTION, task.description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            putExtra(CalendarContract.Events.ALL_DAY, false)
            putExtra(
                CalendarContract.Reminders.MINUTES,
                DEFAULT_REMINDER_MINUTES
            )
        }
    }

    companion object {
        private const val ONE_HOUR_MS = 60 * 60 * 1000L
        private const val DEFAULT_REMINDER_MINUTES = 15
    }
}
