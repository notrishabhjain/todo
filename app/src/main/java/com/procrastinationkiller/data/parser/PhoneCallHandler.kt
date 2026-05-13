package com.procrastinationkiller.data.parser

import android.app.Notification
import android.service.notification.StatusBarNotification
import javax.inject.Inject
import javax.inject.Singleton

data class MissedCallInfo(
    val callerName: String,
    val timestamp: Long
)

@Singleton
class PhoneCallHandler @Inject constructor() {

    companion object {
        val DIALER_PACKAGES = setOf(
            "com.android.dialer",
            "com.samsung.android.dialer",
            "com.google.android.dialer",
            "com.android.incallui",
            "com.android.phone"
        )

        private const val CATEGORY_MISSED_CALL = "missed_call"
        private val MISSED_CALL_PATTERNS = listOf(
            "missed call",
            "missed",
            "unanswered call",
            "you missed a call"
        )
    }

    fun isDialerNotification(packageName: String): Boolean {
        return DIALER_PACKAGES.contains(packageName)
    }

    fun detectMissedCall(sbn: StatusBarNotification): MissedCallInfo? {
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val category = notification.category

        if (!isMissedCallNotification(category, title, text)) {
            return null
        }

        val callerName = extractCallerName(title, text)
        if (callerName.isBlank()) {
            return null
        }

        return MissedCallInfo(
            callerName = callerName,
            timestamp = sbn.postTime
        )
    }

    fun detectMissedCallFromParsed(
        packageName: String,
        title: String,
        text: String,
        category: String?,
        timestamp: Long
    ): MissedCallInfo? {
        if (!isDialerNotification(packageName)) {
            return null
        }

        if (!isMissedCallNotification(category, title, text)) {
            return null
        }

        val callerName = extractCallerName(title, text)
        if (callerName.isBlank()) {
            return null
        }

        return MissedCallInfo(
            callerName = callerName,
            timestamp = timestamp
        )
    }

    fun isMissedCallNotification(category: String?, title: String, text: String): Boolean {
        if (category == CATEGORY_MISSED_CALL) {
            return true
        }

        val lowerTitle = title.lowercase()
        val lowerText = text.lowercase()
        val combined = "$lowerTitle $lowerText"

        return MISSED_CALL_PATTERNS.any { pattern -> combined.contains(pattern) }
    }

    fun extractCallerName(title: String, text: String): String {
        val lowerTitle = title.lowercase()

        // If title contains "missed call" pattern, the caller is likely in the text
        if (MISSED_CALL_PATTERNS.any { lowerTitle.contains(it) }) {
            return text.trim().ifBlank { title.replace(Regex("(?i)missed call(s)?\\s*(from)?\\s*"), "").trim() }
        }

        // Otherwise, the title IS the caller name (common Android dialer behavior)
        return title.trim()
    }

    fun generateTaskTitle(callerName: String): String {
        return "Call back $callerName"
    }
}
