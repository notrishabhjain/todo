package com.procrastinationkiller

import android.app.Application
import android.content.Intent
import android.os.Build
import com.procrastinationkiller.service.ReminderService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ProcrastinationKillerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startReminderService()
    }

    private fun startReminderService() {
        val intent = Intent(this, ReminderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
