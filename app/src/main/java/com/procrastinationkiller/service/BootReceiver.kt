package com.procrastinationkiller.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.procrastinationkiller.data.repository.UserPreferencesRepository
import com.procrastinationkiller.domain.model.ReminderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleReminderWork(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefsRepository = UserPreferencesRepository(context)
                    val mode = try {
                        prefsRepository.reminderMode.first()
                    } catch (_: Exception) {
                        ReminderMode.NORMAL
                    }

                    if (isPersistentNotificationRequired(mode)) {
                        startReminderService(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun isPersistentNotificationRequired(mode: ReminderMode): Boolean {
        return mode == ReminderMode.NUCLEAR || mode == ReminderMode.AGGRESSIVE
    }

    private fun scheduleReminderWork(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            30, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun startReminderService(context: Context) {
        val serviceIntent = Intent(context, ReminderService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
