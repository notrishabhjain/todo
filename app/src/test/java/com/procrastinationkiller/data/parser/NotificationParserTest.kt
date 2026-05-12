package com.procrastinationkiller.data.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NotificationParserTest {

    private lateinit var parser: NotificationParser

    @BeforeEach
    fun setup() {
        parser = NotificationParser()
    }

    @Test
    fun `resolves WhatsApp app type`() {
        assertEquals(AppType.WHATSAPP, parser.resolveAppType("com.whatsapp"))
    }

    @Test
    fun `resolves WhatsApp Business app type`() {
        assertEquals(AppType.WHATSAPP, parser.resolveAppType("com.whatsapp.w4b"))
    }

    @Test
    fun `resolves Telegram app type`() {
        assertEquals(AppType.TELEGRAM, parser.resolveAppType("org.telegram.messenger"))
    }

    @Test
    fun `resolves Gmail app type`() {
        assertEquals(AppType.GMAIL, parser.resolveAppType("com.google.android.gm"))
    }

    @Test
    fun `resolves Slack app type`() {
        assertEquals(AppType.SLACK, parser.resolveAppType("com.Slack"))
    }

    @Test
    fun `resolves SMS app type`() {
        assertEquals(AppType.SMS, parser.resolveAppType("com.google.android.apps.messaging"))
    }

    @Test
    fun `resolves Calendar app type`() {
        assertEquals(AppType.CALENDAR, parser.resolveAppType("com.google.android.calendar"))
    }

    @Test
    fun `resolves unknown app type to OTHER`() {
        assertEquals(AppType.OTHER, parser.resolveAppType("com.unknown.app"))
    }

    @Test
    fun `extracts sender from WhatsApp personal chat`() {
        val sender = parser.extractSender(
            title = "Rahul",
            text = "Please send the file",
            appType = AppType.WHATSAPP,
            isGroupMessage = false
        )
        assertEquals("Rahul", sender)
    }

    @Test
    fun `extracts sender from WhatsApp group message`() {
        val sender = parser.extractSender(
            title = "Priya: Check this out",
            text = "Check this out",
            appType = AppType.WHATSAPP,
            isGroupMessage = true
        )
        assertEquals("Priya", sender)
    }

    @Test
    fun `extracts sender from Gmail`() {
        val sender = parser.extractSender(
            title = "John Smith",
            text = "Meeting at 3pm tomorrow",
            appType = AppType.GMAIL,
            isGroupMessage = false
        )
        assertEquals("John Smith", sender)
    }

    @Test
    fun `extracts sender from Slack with channel`() {
        val sender = parser.extractSender(
            title = "Alice in #general",
            text = "Deploy is done",
            appType = AppType.SLACK,
            isGroupMessage = false
        )
        assertEquals("Alice", sender)
    }

    @Test
    fun `extracts sender from Telegram personal chat`() {
        val sender = parser.extractSender(
            title = "Pavel",
            text = "Lets meet tomorrow",
            appType = AppType.TELEGRAM,
            isGroupMessage = false
        )
        assertEquals("Pavel", sender)
    }

    @Test
    fun `extracts sender from Telegram group message`() {
        val sender = parser.extractSender(
            title = "Ivan: Hey everyone",
            text = "Hey everyone",
            appType = AppType.TELEGRAM,
            isGroupMessage = true
        )
        assertEquals("Ivan", sender)
    }
}
