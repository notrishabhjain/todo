package com.procrastinationkiller.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.procrastinationkiller.data.repository.UserPreferencesRepository
import com.procrastinationkiller.domain.engine.MotivationalMessageProvider
import com.procrastinationkiller.domain.engine.ReminderScheduler
import com.procrastinationkiller.domain.model.ReminderMode
import com.procrastinationkiller.domain.repository.TaskRepository
import com.procrastinationkiller.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderService : Service() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var motivationalMessageProvider: MotivationalMessageProvider

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_UPDATE = "com.procrastinationkiller.UPDATE_REMINDER"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager.createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE -> updateNotification()
            else -> startForegroundWithNotification()
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        serviceScope.launch {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        serviceScope.launch {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun buildNotification(): Notification {
        val tasks = taskRepository.getAllTasks().first()
        val pendingTasks = tasks.filter { it.status == "PENDING" || it.status == "IN_PROGRESS" }
        val highPriorityCount = pendingTasks.count { it.priority == "HIGH" || it.priority == "CRITICAL" }

        val mode = try {
            userPreferencesRepository.reminderMode.first()
        } catch (_: Exception) {
            ReminderMode.AGGRESSIVE
        }
        val message = motivationalMessageProvider.getMessage(mode, pendingTasks.size, highPriorityCount)
        val title = motivationalMessageProvider.getNotificationTitle(mode)

        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_PERSISTENT)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
