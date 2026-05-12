package com.procrastinationkiller.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.procrastinationkiller.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "daily_report_worker"
        const val NOTIFICATION_ID = 3001

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyReportWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val tasks = taskRepository.getAllTasks().first()
            val report = generateReport(tasks)
            postShareNotification(report)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    fun generateReport(tasks: List<com.procrastinationkiller.data.local.entity.TaskEntity>): String {
        val todayStart = getStartOfDay()
        val completedToday = tasks.filter {
            it.status == "COMPLETED" && (it.completedAt ?: 0) >= todayStart
        }
        val pending = tasks.filter { it.status == "PENDING" || it.status == "IN_PROGRESS" }
        val highPriority = pending.filter { it.priority == "HIGH" || it.priority == "CRITICAL" }
        val totalTasks = tasks.size
        val completedTotal = tasks.count { it.status == "COMPLETED" }
        val completionPct = if (totalTasks > 0) (completedTotal * 100 / totalTasks) else 0

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        return buildString {
            appendLine("Daily Productivity Report - $today")
            appendLine("================================")
            appendLine()
            appendLine("Tasks Completed Today: ${completedToday.size}")
            appendLine("Pending Tasks: ${pending.size}")
            appendLine("High Priority Pending: ${highPriority.size}")
            appendLine("Overall Completion Rate: $completionPct%")
            appendLine()
            appendLine("--- Pending High Priority ---")
            for (task in highPriority.take(5)) {
                appendLine("  - ${task.title} [${task.priority}]")
            }
            if (highPriority.size > 5) {
                appendLine("  ... and ${highPriority.size - 5} more")
            }
            appendLine()
            appendLine("Keep up the great work!")
        }
    }

    private fun postShareNotification(report: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Daily Productivity Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share Daily Report")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            chooserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannelManager.CHANNEL_SYSTEM
        )
            .setContentTitle("Daily Report Ready")
            .setContentText("Tap to share your productivity report")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
