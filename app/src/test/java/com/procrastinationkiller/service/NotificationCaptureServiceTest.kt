package com.procrastinationkiller.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NotificationCaptureServiceTest {

    @Test
    fun `WhatsApp checking for messages is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "Checking for new messages"
            )
        )
    }

    @Test
    fun `WhatsApp Web active notification is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "WhatsApp Web is currently active"
            )
        )
    }

    @Test
    fun `WhatsApp end-to-end encrypted notification is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "Security",
                text = "Messages are end-to-end encrypted"
            )
        )
    }

    @Test
    fun `WhatsApp missed voice call is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "Missed voice call",
                text = "Tap to call back"
            )
        )
    }

    @Test
    fun `WhatsApp missed video call is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "Missed video call",
                text = ""
            )
        )
    }

    @Test
    fun `WhatsApp ringing notification is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "Ringing",
                text = "Incoming call"
            )
        )
    }

    @Test
    fun `WhatsApp ongoing voice call is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "Ongoing voice call",
                text = "01:23"
            )
        )
    }

    @Test
    fun `WhatsApp Business system notifications are also filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp.w4b",
                title = "WhatsApp Business",
                text = "Checking for new messages"
            )
        )
    }

    @Test
    fun `regular WhatsApp message is not filtered`() {
        assertFalse(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "John",
                text = "Hey, can you review the PR?"
            )
        )
    }

    @Test
    fun `non-WhatsApp notification is not filtered`() {
        assertFalse(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.google.android.gm",
                title = "Gmail",
                text = "Checking for new messages"
            )
        )
    }

    @Test
    fun `null title and text are not filtered`() {
        assertFalse(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = null,
                text = null
            )
        )
    }

    @Test
    fun `WhatsApp message timer updated is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "Group Chat",
                text = "Message timer updated"
            )
        )
    }

    @Test
    fun `case insensitive matching works for system messages`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "CHECKING FOR NEW MESSAGES"
            )
        )
    }

    @Test
    fun `WhatsApp syncing messages is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "Syncing messages"
            )
        )
    }

    @Test
    fun `WhatsApp backup in progress is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "Backup in progress"
            )
        )
    }

    @Test
    fun `WhatsApp no internet connection is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "No internet connection"
            )
        )
    }

    @Test
    fun `WhatsApp security code changed is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "Security",
                text = "Security code changed"
            )
        )
    }

    @Test
    fun `WhatsApp group membership changes are filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "Group",
                text = "John left the group"
            )
        )
    }

    // WhatsApp message count notification tests

    @Test
    fun `WhatsApp 2 new messages is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "2 new messages"
            )
        )
    }

    @Test
    fun `WhatsApp 5 messages from contact is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "5 messages from John"
            )
        )
    }

    @Test
    fun `WhatsApp 3 new messages from contact is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "3 new messages from Jane Doe"
            )
        )
    }

    @Test
    fun `WhatsApp 1 new message is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "1 new message"
            )
        )
    }

    @Test
    fun `WhatsApp message count in title is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "2 new messages",
                text = null
            )
        )
    }

    @Test
    fun `WhatsApp 10 messages from group is filtered`() {
        assertTrue(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                text = "10 messages from Work Group"
            )
        )
    }

    @Test
    fun `regular message containing number is not filtered`() {
        assertFalse(
            NotificationCaptureService.isWhatsAppSystemNotification(
                packageName = "com.whatsapp",
                title = "John",
                text = "I sent you 3 files yesterday"
            )
        )
    }

    @Test
    fun `isWhatsAppMessageCountNotification returns true for valid patterns`() {
        assertTrue(NotificationCaptureService.isWhatsAppMessageCountNotification("2 new messages"))
        assertTrue(NotificationCaptureService.isWhatsAppMessageCountNotification("5 messages from John"))
        assertTrue(NotificationCaptureService.isWhatsAppMessageCountNotification("1 new message"))
        assertTrue(NotificationCaptureService.isWhatsAppMessageCountNotification("12 messages"))
        assertTrue(NotificationCaptureService.isWhatsAppMessageCountNotification("3 New Messages From Group"))
    }

    @Test
    fun `isWhatsAppMessageCountNotification returns false for non-matching text`() {
        assertFalse(NotificationCaptureService.isWhatsAppMessageCountNotification(null))
        assertFalse(NotificationCaptureService.isWhatsAppMessageCountNotification("Hey, how are you?"))
        assertFalse(NotificationCaptureService.isWhatsAppMessageCountNotification("Please send 2 messages"))
        assertFalse(NotificationCaptureService.isWhatsAppMessageCountNotification("messages"))
        assertFalse(NotificationCaptureService.isWhatsAppMessageCountNotification("new messages"))
    }

    // Gmail system notification filter tests

    @Test
    fun `Gmail syncing mail is filtered`() {
        assertTrue(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm",
                title = "Gmail",
                text = "Syncing mail"
            )
        )
    }

    @Test
    fun `Gmail deleted notification is filtered`() {
        assertTrue(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm",
                title = null,
                text = "Deleted"
            )
        )
    }

    @Test
    fun `Gmail conversation archived is filtered`() {
        assertTrue(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm",
                title = "Gmail",
                text = "Conversation archived"
            )
        )
    }

    @Test
    fun `Gmail undo notification is filtered`() {
        assertTrue(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm",
                title = null,
                text = "Undo"
            )
        )
    }

    @Test
    fun `Gmail marked as read is filtered`() {
        assertTrue(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm",
                title = null,
                text = "Marked as read"
            )
        )
    }

    @Test
    fun `Gmail sending notification is filtered`() {
        assertTrue(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm",
                title = null,
                text = "Sending"
            )
        )
    }

    @Test
    fun `Gmail no new mail is filtered`() {
        assertTrue(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm",
                title = "Gmail",
                text = "No new mail"
            )
        )
    }

    @Test
    fun `Gmail Lite system notification is filtered`() {
        assertTrue(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm.lite",
                title = null,
                text = "Syncing"
            )
        )
    }

    @Test
    fun `Gmail real email is not filtered`() {
        assertFalse(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm",
                title = "John Smith",
                text = "Hey, please review the document I sent"
            )
        )
    }

    @Test
    fun `non-Gmail notification is not filtered by Gmail filter`() {
        assertFalse(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.whatsapp",
                title = null,
                text = "Syncing"
            )
        )
    }

    @Test
    fun `Gmail null title and text is not filtered`() {
        assertFalse(
            NotificationCaptureService.isGmailSystemNotification(
                packageName = "com.google.android.gm",
                title = null,
                text = null
            )
        )
    }
}
