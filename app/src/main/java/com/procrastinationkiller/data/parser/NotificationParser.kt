package com.procrastinationkiller.data.parser

import android.app.Notification
import android.service.notification.StatusBarNotification
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedNotification(
    val packageName: String,
    val sender: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isGroupMessage: Boolean,
    val conversationTitle: String?,
    val appType: AppType
)

enum class AppType {
    WHATSAPP,
    TELEGRAM,
    GMAIL,
    SLACK,
    SMS,
    CALENDAR,
    PHONE_DIALER,
    OTHER
}

@Singleton
class NotificationParser @Inject constructor() {

    fun parse(sbn: StatusBarNotification): ParsedNotification {
        val notification = sbn.notification
        val extras = notification.extras
        val packageName = sbn.packageName

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        val timestamp = sbn.postTime

        val appType = resolveAppType(packageName)
        val isGroupMessage = conversationTitle != null
        val sender = extractSender(title, text, appType, isGroupMessage)

        return ParsedNotification(
            packageName = packageName,
            sender = sender,
            title = title,
            text = text,
            timestamp = timestamp,
            isGroupMessage = isGroupMessage,
            conversationTitle = conversationTitle,
            appType = appType
        )
    }

    fun resolveAppType(packageName: String): AppType {
        val lower = packageName.lowercase()
        return when {
            lower.contains("whatsapp") -> AppType.WHATSAPP
            lower.contains("telegram") -> AppType.TELEGRAM
            lower.contains("google.android.gm") -> AppType.GMAIL
            lower.contains("slack") -> AppType.SLACK
            lower.contains("messaging") || lower.contains("sms") -> AppType.SMS
            lower.contains("calendar") -> AppType.CALENDAR
            lower.contains("dialer") || lower.contains("incallui") || lower == "com.android.phone" -> AppType.PHONE_DIALER
            else -> AppType.OTHER
        }
    }

    fun extractSender(
        title: String,
        text: String,
        appType: AppType,
        isGroupMessage: Boolean
    ): String {
        return when (appType) {
            AppType.WHATSAPP -> {
                if (isGroupMessage && title.contains(":")) {
                    // Group message format: "Sender @ Group" or "Sender: message"
                    title.substringBefore(":").trim()
                } else {
                    title
                }
            }
            AppType.GMAIL -> {
                // Gmail title is usually the sender
                title
            }
            AppType.TELEGRAM -> {
                if (isGroupMessage && title.contains(":")) {
                    title.substringBefore(":").trim()
                } else {
                    title
                }
            }
            AppType.SLACK -> {
                title.substringBefore(" in ").trim()
            }
            else -> title
        }
    }
}
