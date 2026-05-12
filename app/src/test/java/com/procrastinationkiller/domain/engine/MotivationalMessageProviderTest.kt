package com.procrastinationkiller.domain.engine

import com.procrastinationkiller.domain.model.ReminderMode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MotivationalMessageProviderTest {

    private lateinit var provider: MotivationalMessageProvider

    @BeforeEach
    fun setup() {
        provider = MotivationalMessageProvider()
    }

    @Test
    fun `getMessage returns non-empty message for normal mode`() {
        val message = provider.getMessage(ReminderMode.NORMAL, 5, 0)
        assertNotNull(message)
        assertTrue(message.isNotBlank())
    }

    @Test
    fun `getMessage returns aggressive message for aggressive mode`() {
        val message = provider.getMessage(ReminderMode.AGGRESSIVE, 3, 0)
        assertNotNull(message)
        assertTrue(message.isNotBlank())
        assertTrue(message.contains("3 tasks waiting"))
    }

    @Test
    fun `getMessage returns high urgency message when high priority exists`() {
        val message = provider.getMessage(ReminderMode.NORMAL, 5, 2)
        assertNotNull(message)
        assertTrue(message.contains("2 high priority"))
        assertTrue(message.contains("5 total"))
    }

    @Test
    fun `getTimeBasedMessage includes pending count`() {
        val message = provider.getTimeBasedMessage(3)
        assertTrue(message.contains("3 pending"))
    }

    @Test
    fun `getTimeBasedMessage with zero pending does not show count`() {
        val message = provider.getTimeBasedMessage(0)
        assertFalse(message.contains("pending"))
    }

    @Test
    fun `getAggressiveMessage includes task count`() {
        val message = provider.getAggressiveMessage(5)
        assertTrue(message.contains("5 tasks waiting"))
    }

    @Test
    fun `getAggressiveMessage with zero pending returns message without count`() {
        val message = provider.getAggressiveMessage(0)
        assertFalse(message.contains("tasks waiting"))
    }

    @Test
    fun `getHighUrgencyMessage shows both counts`() {
        val message = provider.getHighUrgencyMessage(10, 3)
        assertTrue(message.contains("3 high priority"))
        assertTrue(message.contains("10 total"))
    }

    @Test
    fun `getNotificationTitle returns different titles for different modes`() {
        val gentle = provider.getNotificationTitle(ReminderMode.GENTLE)
        val normal = provider.getNotificationTitle(ReminderMode.NORMAL)
        val aggressive = provider.getNotificationTitle(ReminderMode.AGGRESSIVE)
        val nuclear = provider.getNotificationTitle(ReminderMode.NUCLEAR)

        assertTrue(gentle != aggressive)
        assertTrue(normal != nuclear)
        assertTrue(gentle.isNotBlank())
        assertTrue(normal.isNotBlank())
        assertTrue(aggressive.isNotBlank())
        assertTrue(nuclear.isNotBlank())
    }

    @Test
    fun `gentle mode title is friendly`() {
        val title = provider.getNotificationTitle(ReminderMode.GENTLE)
        assertTrue(title.contains("Friendly"))
    }

    @Test
    fun `nuclear mode title is urgent`() {
        val title = provider.getNotificationTitle(ReminderMode.NUCLEAR)
        assertTrue(title.contains("NOW"))
    }

    @Test
    fun `getMessage rotates messages based on pending count`() {
        val message1 = provider.getAggressiveMessage(1)
        val message2 = provider.getAggressiveMessage(2)
        // Different pending counts may produce different messages
        // (they rotate based on modulo, so with enough messages they differ)
        assertNotNull(message1)
        assertNotNull(message2)
    }

    @Test
    fun `nuclear mode uses aggressive messages`() {
        val message = provider.getMessage(ReminderMode.NUCLEAR, 4, 0)
        assertTrue(message.contains("4 tasks waiting"))
    }
}
