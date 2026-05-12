package com.procrastinationkiller.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.procrastinationkiller.domain.usecase.TaskExtractionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationCaptureService : NotificationListenerService() {

    @Inject
    lateinit var taskExtractionUseCase: TaskExtractionUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Skip system notifications and our own notifications
        if (isSystemNotification(sbn) || sbn.packageName == packageName) {
            return
        }

        serviceScope.launch {
            taskExtractionUseCase.processNotification(sbn)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed on notification removal
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun isSystemNotification(sbn: StatusBarNotification): Boolean {
        val systemPackages = listOf(
            "android",
            "com.android.systemui",
            "com.android.providers"
        )
        return systemPackages.any { sbn.packageName.startsWith(it) }
    }
}
