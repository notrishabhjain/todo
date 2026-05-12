package com.procrastinationkiller.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.procrastinationkiller.data.repository.UserPreferencesRepository
import com.procrastinationkiller.domain.engine.MotivationalMessageProvider
import com.procrastinationkiller.domain.engine.ReminderScheduler
import com.procrastinationkiller.domain.model.ReminderMode
import com.procrastinationkiller.domain.repository.TaskRepository
import com.procrastinationkiller.presentation.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val motivationalMessageProvider: MotivationalMessageProvider,
    private val reminderScheduler: ReminderScheduler,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "reminder_worker"
        const val NOTIFICATION_ID = 2001
    }

    override suspend fun doWork(): Result {
        val mode = try {
            userPreferencesRepository.reminderMode.first()
        } catch (_: Exception) {
            ReminderMode.NORMAL
        }
        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (!reminderScheduler.shouldShowReminder(mode, hourOfDay)) {
            return Result.success()
        }

        val tasks = taskRepository.getAllTasks().first()
        val pendingTasks = tasks.filter { it.status == "PENDING" || it.status == "IN_PROGRESS" }

        if (pendingTasks.isEmpty()) {
            return Result.success()
        }

        val highPriorityCount = pendingTasks.count { it.priority == "HIGH" || it.priority == "CRITICAL" }
        val message = motivationalMessageProvider.getMessage(mode, pendingTasks.size, highPriorityCount)
        val title = motivationalMessageProvider.getNotificationTitle(mode)

        showNotification(title, message)
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannelManager.CHANNEL_REMINDERS
        )
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }
}
