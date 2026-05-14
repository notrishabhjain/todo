package com.procrastinationkiller.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.procrastinationkiller.data.repository.UserPreferencesRepository
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

    companion object {
        val whatsAppSystemMessages = setOf(
            "checking for new messages",
            "whatsapp web is currently active",
            "waiting for this message",
            "this message was deleted",
            "messages you send to this group are now secured",
            "end-to-end encrypted",
            "ringing",
            "ongoing voice call",
            "ongoing video call",
            "missed voice call",
            "missed video call",
            "you might have new messages",
            "message timer updated",
            "syncing messages",
            "syncing",
            "backup in progress",
            "restoring messages",
            "downloading media",
            "uploading media",
            "connecting",
            "updating",
            "installing update",
            "could not connect",
            "no internet connection",
            "tap for more info",
            "tap to learn more",
            "security code changed",
            "your security code with",
            "business account",
            "changed their phone number",
            "left the group",
            "joined the group",
            "added you",
            "removed you",
            "changed the subject",
            "changed this group"
        )

        private val whatsAppPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

        private val MESSAGE_COUNT_PATTERN = Regex("^\\d+\\s+(new\\s+)?messages?(\\s+from.*)?$", RegexOption.IGNORE_CASE)

        fun isWhatsAppMessageCountNotification(text: String?): Boolean {
            if (text == null) return false
            return MESSAGE_COUNT_PATTERN.matches(text.trim())
        }

        fun isWhatsAppSystemNotification(packageName: String, title: String?, text: String?): Boolean {
            if (packageName !in whatsAppPackages) return false

            val lowerTitle = title?.lowercase() ?: ""
            val lowerText = text?.lowercase() ?: ""

            // Check message count summaries (e.g., "2 new messages", "5 messages from John")
            if (isWhatsAppMessageCountNotification(title) || isWhatsAppMessageCountNotification(text)) {
                return true
            }

            return whatsAppSystemMessages.any { systemMsg ->
                lowerTitle.contains(systemMsg) || lowerText.contains(systemMsg) ||
                    lowerTitle == systemMsg || lowerText == systemMsg
            }
        }

        private val gmailSystemMessages = setOf(
            "syncing",
            "syncing mail",
            "sync",
            "account sync",
            "deleted",
            "archived",
            "conversation deleted",
            "conversation archived",
            "message deleted",
            "message archived",
            "undo",
            "undone",
            "removed",
            "moved to trash",
            "marked as read",
            "marked as spam",
            "muted",
            "snoozed",
            "label added",
            "label removed",
            "sending",
            "sent",
            "uploading attachment",
            "checking for mail",
            "no new mail"
        )

        private val gmailPackages = setOf(
            "com.google.android.gm",
            "com.google.android.gm.lite"
        )

        fun isGmailSystemNotification(packageName: String, title: String?, text: String?): Boolean {
            if (packageName !in gmailPackages) return false

            val lowerTitle = title?.lowercase()?.trim() ?: ""
            val lowerText = text?.lowercase()?.trim() ?: ""

            return gmailSystemMessages.any { systemMsg ->
                lowerTitle == systemMsg || lowerText == systemMsg ||
                    lowerTitle.startsWith(systemMsg) || lowerText.startsWith(systemMsg)
            }
        }
    }

    @Inject
    lateinit var taskExtractionUseCase: TaskExtractionUseCase

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var monitoredApps: Set<String> = emptySet()

    // Tracks whether the monitoredApps set has been loaded from preferences at least once.
    // While not yet loaded (empty set), all notifications are allowed through to avoid
    // dropping notifications during the startup race window.
    @Volatile
    private var monitoredAppsLoaded: Boolean = false

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            userPreferencesRepository.monitoredApps.collect { apps ->
                monitoredApps = apps
                monitoredAppsLoaded = true
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Skip system notifications and our own notifications
        if (isSystemNotification(sbn) || sbn.packageName == packageName) {
            return
        }

        // Skip notifications from apps not in the monitored set.
        // When monitoredApps hasn't been loaded yet, allow all notifications through
        // to avoid dropping notifications during the startup race window.
        if (monitoredAppsLoaded && sbn.packageName !in monitoredApps) {
            return
        }

        // Filter WhatsApp system notifications
        if (isWhatsAppSystemNotification(sbn)) {
            return
        }

        // Filter Gmail system notifications
        if (isGmailSystemNotification(sbn)) {
            return
        }

        serviceScope.launch {
            taskExtractionUseCase.processNotification(sbn, sbnKey = sbn.key)
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
        val allowedDialerPackages = setOf(
            "com.android.dialer",
            "com.samsung.android.dialer",
            "com.google.android.dialer",
            "com.android.phone",
            "com.android.incallui"
        )

        if (allowedDialerPackages.contains(sbn.packageName)) {
            return false
        }

        val systemPackages = listOf(
            "android",
            "com.android.systemui",
            "com.android.providers"
        )
        return systemPackages.any { sbn.packageName.startsWith(it) }
    }

    private fun isWhatsAppSystemNotification(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        return Companion.isWhatsAppSystemNotification(sbn.packageName, title, text)
    }

    private fun isGmailSystemNotification(sbn: StatusBarNotification): Boolean {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        return Companion.isGmailSystemNotification(sbn.packageName, title, text)
    }
}
