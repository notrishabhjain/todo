package com.procrastinationkiller.domain.engine

import com.procrastinationkiller.domain.model.ReminderMode
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotivationalMessageProvider @Inject constructor() {

    private val morningMessages = listOf(
        "Rise and conquer! You have tasks waiting for your attention.",
        "Good morning! Let's make today productive.",
        "New day, new opportunities. Time to tackle your tasks!",
        "Start strong today. Your future self will thank you.",
        "Morning! Don't let procrastination win today."
    )

    private val afternoonMessages = listOf(
        "Keep the momentum going! You're doing great.",
        "Halfway through the day. Time to check off more tasks.",
        "Don't slow down now. Push through the afternoon slump!",
        "Your tasks won't complete themselves. Let's go!",
        "Stay focused. The finish line is closer than you think."
    )

    private val eveningMessages = listOf(
        "End the day strong! Finish what you started.",
        "Before you relax, check your pending tasks.",
        "Wind down productively. Clear those remaining items.",
        "Almost done for the day. Can you knock out one more?",
        "Evening check-in: Don't carry unfinished tasks to tomorrow."
    )

    private val aggressiveMessages = listOf(
        "STOP SCROLLING. You have unfinished work.",
        "Every minute you delay is a minute wasted. Act NOW.",
        "Procrastination is the thief of time. Don't be robbed.",
        "You said you'd do it later. Later is NOW.",
        "No more excuses. Open your task list and START.",
        "Your deadlines don't care about your comfort zone.",
        "Action beats intention every single time. Move!",
        "The discomfort of discipline is less than the pain of regret."
    )

    private val highUrgencyMessages = listOf(
        "CRITICAL: You have high-priority tasks past due!",
        "Urgent tasks are piling up. Address them immediately!",
        "High-priority items need your attention RIGHT NOW.",
        "Don't let urgent tasks become emergencies. Act now!",
        "Your most important tasks can't wait any longer."
    )

    fun getMessage(
        mode: ReminderMode,
        pendingCount: Int,
        highPriorityCount: Int
    ): String {
        if (highPriorityCount > 0) {
            return getHighUrgencyMessage(pendingCount, highPriorityCount)
        }

        return when (mode) {
            ReminderMode.AGGRESSIVE, ReminderMode.NUCLEAR -> getAggressiveMessage(pendingCount)
            else -> getTimeBasedMessage(pendingCount)
        }
    }

    fun getTimeBasedMessage(pendingCount: Int): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val messages = when {
            hour in 5..11 -> morningMessages
            hour in 12..17 -> afternoonMessages
            else -> eveningMessages
        }
        val message = messages[pendingCount.mod(messages.size)]
        return if (pendingCount > 0) "$message ($pendingCount pending)" else message
    }

    fun getAggressiveMessage(pendingCount: Int): String {
        val message = aggressiveMessages[pendingCount.mod(aggressiveMessages.size)]
        return if (pendingCount > 0) "$message ($pendingCount tasks waiting)" else message
    }

    fun getHighUrgencyMessage(pendingCount: Int, highPriorityCount: Int): String {
        val message = highUrgencyMessages[pendingCount.mod(highUrgencyMessages.size)]
        return "$message ($highPriorityCount high priority out of $pendingCount total)"
    }

    fun getNotificationTitle(mode: ReminderMode): String {
        return when (mode) {
            ReminderMode.GENTLE -> "Friendly Reminder"
            ReminderMode.NORMAL -> "Task Reminder"
            ReminderMode.AGGRESSIVE -> "Action Required!"
            ReminderMode.NUCLEAR -> "DO IT NOW!"
        }
    }
}
