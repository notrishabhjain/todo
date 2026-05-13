package com.procrastinationkiller.domain.engine.semantic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConversationFlowAnalyzerTest {

    private lateinit var analyzer: ConversationFlowAnalyzer

    @BeforeEach
    fun setup() {
        analyzer = ConversationFlowAnalyzer()
        analyzer.clear()
    }

    @Test
    fun `feeding sequential messages from same sender builds context`() {
        analyzer.addMessage("Boss", "We need to send the report", 1000L)
        analyzer.addMessage("Boss", "The client is waiting", 2000L)

        val messages = analyzer.getRecentMessages("Boss")
        assertEquals(2, messages.size)
        assertEquals("We need to send the report", messages[0].text)
    }

    @Test
    fun `do that thing after we need to send the report resolves to report context`() {
        analyzer.addMessage("Boss", "We need to send the report", 1000L)

        val resolved = analyzer.resolveContext("Boss", "do that thing now")

        assertNotNull(resolved)
        resolved!!
        assertEquals("We need to send the report", resolved.text)
        assertEquals("Boss", resolved.sender)
    }

    @Test
    fun `no reference in message returns null context`() {
        analyzer.addMessage("Boss", "We need to send the report", 1000L)

        val resolved = analyzer.resolveContext("Boss", "finish the presentation")

        assertNull(resolved)
    }

    @Test
    fun `different sender messages are isolated`() {
        analyzer.addMessage("Alice", "Send the invoice", 1000L)
        analyzer.addMessage("Bob", "Check the server", 2000L)

        val aliceMessages = analyzer.getRecentMessages("Alice")
        val bobMessages = analyzer.getRecentMessages("Bob")

        assertEquals(1, aliceMessages.size)
        assertEquals(1, bobMessages.size)
        assertEquals("Send the invoice", aliceMessages[0].text)
        assertEquals("Check the server", bobMessages[0].text)
    }

    @Test
    fun `sliding window keeps only last 5 messages`() {
        for (i in 1..7) {
            analyzer.addMessage("Boss", "message $i send", i.toLong())
        }

        val messages = analyzer.getRecentMessages("Boss")
        assertEquals(5, messages.size)
        assertEquals("message 3 send", messages[0].text)
        assertEquals("message 7 send", messages[4].text)
    }

    @Test
    fun `resolves it reference to last actionable message`() {
        analyzer.addMessage("Manager", "Please review the PR", 1000L)
        analyzer.addMessage("Manager", "Good morning team", 2000L)

        val resolved = analyzer.resolveContext("Manager", "Did you do it?")

        assertNotNull(resolved)
        resolved!!
        // Should resolve to the message with action verb "review"
        assertEquals("Please review the PR", resolved.text)
    }

    @Test
    fun `case insensitive sender matching`() {
        analyzer.addMessage("Boss", "send the report", 1000L)

        val messages = analyzer.getRecentMessages("boss")
        assertEquals(1, messages.size)
    }

    @Test
    fun `LRU eviction removes oldest-accessed sender when exceeding 100 entries`() {
        // Add 101 distinct senders
        for (i in 1..101) {
            analyzer.addMessage("Sender$i", "message from sender $i", i.toLong())
        }

        // The first sender (oldest-accessed) should have been evicted
        val evictedMessages = analyzer.getRecentMessages("Sender1")
        assertTrue(evictedMessages.isEmpty(), "Sender1 should have been evicted as the oldest-accessed entry")

        // The most recent senders should still be present
        val recentMessages = analyzer.getRecentMessages("Sender101")
        assertEquals(1, recentMessages.size, "Sender101 should still be present")

        // Sender2 should still be present (it was added second, so after eviction of Sender1, it stays)
        val sender2Messages = analyzer.getRecentMessages("Sender2")
        assertEquals(1, sender2Messages.size, "Sender2 should still be present after only Sender1 was evicted")
    }

    @Test
    fun `LRU eviction respects access order not insertion order`() {
        // Add 100 distinct senders
        for (i in 1..100) {
            analyzer.addMessage("Sender$i", "message from sender $i", i.toLong())
        }

        // Access Sender1 again to make it most recently used
        analyzer.addMessage("Sender1", "another message", 200L)

        // Add a new sender to trigger eviction
        analyzer.addMessage("NewSender", "new message", 300L)

        // Sender1 should NOT be evicted because it was most recently accessed
        val sender1Messages = analyzer.getRecentMessages("Sender1")
        assertTrue(sender1Messages.isNotEmpty(), "Sender1 should not be evicted since it was recently accessed")

        // Sender2 should be evicted as the oldest-accessed entry
        val sender2Messages = analyzer.getRecentMessages("Sender2")
        assertTrue(sender2Messages.isEmpty(), "Sender2 should have been evicted as the oldest-accessed entry")
    }
}
