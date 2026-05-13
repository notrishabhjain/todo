package com.procrastinationkiller.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_REMINDERS = "task_reminders"
        const val CHANNEL_SUGGESTIONS = "task_suggestions"
        const val CHANNEL_SYSTEM = "system_notifications"
        const val CHANNEL_PERSISTENT = "persistent_reminder"
    }

    fun createNotificationChannels() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Periodic reminders for pending tasks"
            enableVibration(true)
            setShowBadge(true)
        }

        val suggestionChannel = NotificationChannel(
            CHANNEL_SUGGESTIONS,
            "Task Suggestions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Suggested tasks from notifications"
            enableVibration(true)
            setShowBadge(true)
        }

        val systemChannel = NotificationChannel(
            CHANNEL_SYSTEM,
            "System Notifications",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "System and service notifications"
            enableVibration(false)
            setShowBadge(false)
        }

        val persistentChannel = NotificationChannel(
            CHANNEL_PERSISTENT,
            "Persistent Reminder",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Always-on notification showing pending tasks"
            enableVibration(false)
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(
            listOf(reminderChannel, suggestionChannel, systemChannel, persistentChannel)
        )
    }
}
