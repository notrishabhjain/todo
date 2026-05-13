package com.procrastinationkiller.data.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PhoneCallHandlerTest {

    private lateinit var handler: PhoneCallHandler

    @BeforeEach
    fun setup() {
        handler = PhoneCallHandler()
    }

    @Test
    fun `isDialerNotification returns true for android dialer`() {
        assertTrue(handler.isDialerNotification("com.android.dialer"))
    }

    @Test
    fun `isDialerNotification returns true for samsung dialer`() {
        assertTrue(handler.isDialerNotification("com.samsung.android.dialer"))
    }

    @Test
    fun `isDialerNotification returns true for google dialer`() {
        assertTrue(handler.isDialerNotification("com.google.android.dialer"))
    }

    @Test
    fun `isDialerNotification returns true for incallui`() {
        assertTrue(handler.isDialerNotification("com.android.incallui"))
    }

    @Test
    fun `isDialerNotification returns true for android phone`() {
        assertTrue(handler.isDialerNotification("com.android.phone"))
    }

    @Test
    fun `isDialerNotification returns false for non-dialer package`() {
        assertFalse(handler.isDialerNotification("com.whatsapp"))
    }

    @Test
    fun `isMissedCallNotification detects missed_call category`() {
        assertTrue(handler.isMissedCallNotification("missed_call", "John", ""))
    }

    @Test
    fun `isMissedCallNotification detects missed call in title`() {
        assertTrue(handler.isMissedCallNotification(null, "Missed call", "from Mom"))
    }

    @Test
    fun `isMissedCallNotification detects missed call in text`() {
        assertTrue(handler.isMissedCallNotification(null, "Mom", "Missed call"))
    }

    @Test
    fun `isMissedCallNotification returns false for non-missed-call`() {
        assertFalse(handler.isMissedCallNotification(null, "John", "Calling..."))
    }

    @Test
    fun `extractCallerName extracts name from title when no missed call pattern in title`() {
        val name = handler.extractCallerName("John Smith", "Missed call")
        assertEquals("John Smith", name)
    }

    @Test
    fun `extractCallerName extracts name from text when title has missed call pattern`() {
        val name = handler.extractCallerName("Missed call", "Mom")
        assertEquals("Mom", name)
    }

    @Test
    fun `extractCallerName extracts name from title pattern Missed call from Name`() {
        val name = handler.extractCallerName("Missed call from Dad", "")
        assertEquals("Dad", name)
    }

    @Test
    fun `generateTaskTitle creates correct title`() {
        assertEquals("Call back John", handler.generateTaskTitle("John"))
    }

    @Test
    fun `generateTaskTitle works with full name`() {
        assertEquals("Call back John Smith", handler.generateTaskTitle("John Smith"))
    }

    @Test
    fun `detectMissedCallFromParsed returns MissedCallInfo for valid missed call`() {
        val result = handler.detectMissedCallFromParsed(
            packageName = "com.android.dialer",
            title = "Mom",
            text = "Missed call",
            category = null,
            timestamp = 1234567890L
        )

        assertNotNull(result)
        assertEquals("Mom", result!!.callerName)
        assertEquals(1234567890L, result.timestamp)
    }

    @Test
    fun `detectMissedCallFromParsed returns MissedCallInfo for missed_call category`() {
        val result = handler.detectMissedCallFromParsed(
            packageName = "com.google.android.dialer",
            title = "Jane Doe",
            text = "",
            category = "missed_call",
            timestamp = 9876543210L
        )

        assertNotNull(result)
        assertEquals("Jane Doe", result!!.callerName)
    }

    @Test
    fun `detectMissedCallFromParsed returns null for non-dialer package`() {
        val result = handler.detectMissedCallFromParsed(
            packageName = "com.whatsapp",
            title = "Mom",
            text = "Missed call",
            category = null,
            timestamp = 1234567890L
        )

        assertNull(result)
    }

    @Test
    fun `detectMissedCallFromParsed returns null for non-missed-call notification`() {
        val result = handler.detectMissedCallFromParsed(
            packageName = "com.android.dialer",
            title = "John",
            text = "Ongoing call",
            category = null,
            timestamp = 1234567890L
        )

        assertNull(result)
    }

    @Test
    fun `detectMissedCallFromParsed returns null for blank caller name`() {
        val result = handler.detectMissedCallFromParsed(
            packageName = "com.android.dialer",
            title = "Missed call",
            text = "",
            category = null,
            timestamp = 1234567890L
        )

        // Title contains "missed call" and text is empty, extractCallerName tries to parse
        // from the title pattern "Missed call from ..." - nothing after so blank
        assertNull(result)
    }
}
