package com.procrastinationkiller.domain.engine.semantic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Calendar

class ImplicitDeadlineResolverTest {

    private lateinit var resolver: ImplicitDeadlineResolver

    @BeforeEach
    fun setup() {
        resolver = ImplicitDeadlineResolver()
    }

    @Test
    fun `before end of day resolves to 6PM today`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val result = resolver.resolveWithCalendar("Please finish before end of day", now)

        assertNotNull(result)
        result!!
        val resolved = Calendar.getInstance().apply { timeInMillis = result.resolvedTimestamp }
        assertEquals(18, resolved.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resolved.get(Calendar.MINUTE))
        assertEquals("before end of day", result.sourcePhrase)
    }

    @Test
    fun `first thing tomorrow resolves to 9AM next day`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val result = resolver.resolveWithCalendar("I need this first thing tomorrow", now)

        assertNotNull(result)
        result!!
        val resolved = Calendar.getInstance().apply { timeInMillis = result.resolvedTimestamp }
        assertEquals(9, resolved.get(Calendar.HOUR_OF_DAY))
        // Should be next day
        val expectedDay = (now.get(Calendar.DAY_OF_YEAR) + 1)
        assertEquals(expectedDay, resolved.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `this weekend resolves to Saturday 10AM`() {
        // Use a Wednesday
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val result = resolver.resolveWithCalendar("Let's do this weekend", now)

        assertNotNull(result)
        result!!
        val resolved = Calendar.getInstance().apply { timeInMillis = result.resolvedTimestamp }
        assertEquals(Calendar.SATURDAY, resolved.get(Calendar.DAY_OF_WEEK))
        assertEquals(10, resolved.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `plain text with no time reference returns null`() {
        val now = Calendar.getInstance()
        val result = resolver.resolveWithCalendar("Please send the report to the client", now)
        assertNull(result)
    }

    @Test
    fun `ASAP resolves to 1 hour from now`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val result = resolver.resolveWithCalendar("Need this ASAP", now)

        assertNotNull(result)
        result!!
        val resolved = Calendar.getInstance().apply { timeInMillis = result.resolvedTimestamp }
        assertEquals(15, resolved.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resolved.get(Calendar.MINUTE))
    }

    @Test
    fun `before I leave resolves to 6PM today`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val result = resolver.resolveWithCalendar("Get this done before I leave", now)

        assertNotNull(result)
        result!!
        val resolved = Calendar.getInstance().apply { timeInMillis = result.resolvedTimestamp }
        assertEquals(18, resolved.get(Calendar.HOUR_OF_DAY))
    }
}
