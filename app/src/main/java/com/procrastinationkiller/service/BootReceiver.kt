package com.procrastinationkiller.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleReminderWork(context)
            startReminderService(context)
        }
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
