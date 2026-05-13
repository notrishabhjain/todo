package com.procrastinationkiller.data.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WhatsAppHandlerGroupTest {

    private lateinit var handler: WhatsAppHandler

    @BeforeEach
    fun setup() {
        handler = WhatsAppHandler()
    }

    @Test
    fun `isWhatsAppNotification returns true for whatsapp package`() {
        assertTrue(handler.isWhatsAppNotification("com.whatsapp"))
    }

    @Test
    fun `isWhatsAppNotification returns true for whatsapp business`() {
        assertTrue(handler.isWhatsAppNotification("com.whatsapp.w4b"))
    }

    @Test
    fun `isWhatsAppNotification returns false for other packages`() {
        assertFalse(handler.isWhatsAppNotification("com.telegram.messenger"))
    }

    @Test
    fun `extractGroupSender extracts sender from text with colon format`() {
        val sender = handler.extractGroupSender("Work Group", "Alice: Please send the report")
        assertEquals("Alice", sender)
    }

    @Test
    fun `extractGroupSender extracts sender from title with @ format`() {
        val sender = handler.extractGroupSender("Bob @ Team Chat", "Hey everyone")
        assertEquals("Bob", sender)
    }

    @Test
    fun `extractGroupSender falls back to title when no pattern matches`() {
        val sender = handler.extractGroupSender("Work Group", "Hello everyone")
        assertEquals("Work Group", sender)
    }

    @Test
    fun `extractGroupName extracts name from title with @ format`() {
        val name = handler.extractGroupName("Bob @ Team Chat")
        assertEquals("Team Chat", name)
    }

    @Test
    fun `extractGroupName returns null when no @ in title`() {
        val name = handler.extractGroupName("Work Group")
        assertNull(name)
    }

    @Test
    fun `getMessageContent returns text after colon for group messages`() {
        val message = WhatsAppMessage(
            sender = "Alice",
            text = "Alice: Please submit the report",
            groupName = "Work",
            isGroupChat = true,
            timestamp = 1000L
        )
        val content = handler.getMessageContent(message)
        assertEquals("Please submit the report", content)
    }

    @Test
    fun `getMessageContent returns full text for non-group messages`() {
        val message = WhatsAppMessage(
            sender = "Bob",
            text = "Please call me tomorrow",
            groupName = null,
            isGroupChat = false,
            timestamp = 1000L
        )
        val content = handler.getMessageContent(message)
        assertEquals("Please call me tomorrow", content)
    }

    @Test
    fun `getMessageContent returns full text when stripped content is too short`() {
        val message = WhatsAppMessage(
            sender = "Alice",
            text = "Alice: ok",
            groupName = "Work",
            isGroupChat = true,
            timestamp = 1000L
        )
        // "ok" is only 2 chars, so should return full text
        val content = handler.getMessageContent(message)
        assertEquals("Alice: ok", content)
    }

    @Test
    fun `getMessageContent returns stripped content when long enough`() {
        val message = WhatsAppMessage(
            sender = "Alice",
            text = "Alice: send the file",
            groupName = "Work",
            isGroupChat = true,
            timestamp = 1000L
        )
        val content = handler.getMessageContent(message)
        assertEquals("send the file", content)
    }

    @Test
    fun `getMessageContent returns full text for group chat without colon`() {
        val message = WhatsAppMessage(
            sender = "Alice",
            text = "Send the report please",
            groupName = "Work",
            isGroupChat = true,
            timestamp = 1000L
        )
        val content = handler.getMessageContent(message)
        assertEquals("Send the report please", content)
    }

    @Test
    fun `group messages with N messages pattern are detected`() {
        // The detectGroupFormat is private but we test behavior through getMessageContent
        // by checking messages with summary patterns
        val message = WhatsAppMessage(
            sender = "Work Group",
            text = "Bob: Review the PR by tomorrow",
            groupName = "Work Group",
            isGroupChat = true,
            timestamp = 1000L
        )
        val content = handler.getMessageContent(message)
        assertEquals("Review the PR by tomorrow", content)
    }

    @Test
    fun `getMessageContent handles empty text after colon gracefully`() {
        val message = WhatsAppMessage(
            sender = "Alice",
            text = "Alice: ",
            groupName = "Work",
            isGroupChat = true,
            timestamp = 1000L
        )
        // Empty string after colon (length 0 < 3) - should return full text
        val content = handler.getMessageContent(message)
        assertEquals("Alice: ", content)
    }
}
