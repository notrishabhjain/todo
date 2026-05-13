package com.procrastinationkiller.data.parser

import android.app.Notification
import android.service.notification.StatusBarNotification
import javax.inject.Inject
import javax.inject.Singleton

data class WhatsAppMessage(
    val sender: String,
    val text: String,
    val groupName: String?,
    val isGroupChat: Boolean,
    val timestamp: Long
)

@Singleton
class WhatsAppHandler @Inject constructor() {

    companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    }

    fun isWhatsAppNotification(packageName: String): Boolean {
        return packageName == WHATSAPP_PACKAGE || packageName == WHATSAPP_BUSINESS_PACKAGE
    }

    fun parseWhatsAppNotification(sbn: StatusBarNotification): WhatsAppMessage {
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

        val isGroupChat = conversationTitle != null || title.contains("@") || detectGroupFormat(title, text)

        val sender: String
        val groupName: String?

        if (isGroupChat) {
            // In group chats, the format can be "Sender @ Group" or conversation title is the group name
            sender = extractGroupSender(title, text)
            groupName = conversationTitle ?: extractGroupName(title)
        } else {
            sender = title
            groupName = null
        }

        return WhatsAppMessage(
            sender = sender,
            text = text,
            groupName = groupName,
            isGroupChat = isGroupChat,
            timestamp = sbn.postTime
        )
    }

    fun extractGroupSender(title: String, text: String): String {
        // Pattern: "Sender: message" in text or "Sender @ Group" in title
        return when {
            text.contains(":") -> text.substringBefore(":").trim()
            title.contains("@") -> title.substringBefore("@").trim()
            else -> title
        }
    }

    fun extractGroupName(title: String): String? {
        return when {
            title.contains("@") -> title.substringAfter("@").trim()
            else -> null
        }
    }

    private fun detectGroupFormat(title: String, text: String): Boolean {
        // WhatsApp group messages often have format "Name: message" in text
        // or multiple messages indicator
        // Also detect if text starts with a name followed by colon (common group format)
        if (text.contains(": ") && !title.contains(":")) {
            return true
        }
        // Check for "N messages" pattern indicating group summary
        if (text.matches(Regex("\\d+\\s+messages?"))) {
            return true
        }
        return false
    }

    fun getMessageContent(whatsAppMessage: WhatsAppMessage): String {
        // For group messages, the text may include "Sender: actual message"
        val text = whatsAppMessage.text
        if (whatsAppMessage.isGroupChat && text.contains(":")) {
            val afterColon = text.substringAfter(":").trim()
            // If the content after stripping sender prefix is too short or empty,
            // return the full text to give the extraction engine more context
            if (afterColon.length < 3) {
                return text
            }
            return afterColon
        }
        return text
    }
}
