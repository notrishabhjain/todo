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
}
