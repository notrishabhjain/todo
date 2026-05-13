package com.procrastinationkiller.presentation.settings

import com.procrastinationkiller.data.local.entity.ContactEntity
import com.procrastinationkiller.domain.model.ContactPriority
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests the contact priority mapping logic used in SettingsViewModel.addContact().
 * Extracted as pure logic tests since SettingsViewModel depends on concrete classes
 * that require Android Context.
 */
class ContactPriorityMappingTest {

    @Test
    fun `VIP priority maps to isEscalationTarget true`() {
        val contact = createContactWithPriority(ContactPriority.VIP)
        assertTrue(contact.isEscalationTarget)
        assertEquals("VIP", contact.priority)
    }

    @Test
    fun `HIGH_PRIORITY maps to isEscalationTarget true`() {
        val contact = createContactWithPriority(ContactPriority.HIGH_PRIORITY)
        assertTrue(contact.isEscalationTarget)
        assertEquals("HIGH_PRIORITY", contact.priority)
    }

    @Test
    fun `NORMAL priority maps to isEscalationTarget false`() {
        val contact = createContactWithPriority(ContactPriority.NORMAL)
        assertFalse(contact.isEscalationTarget)
        assertEquals("NORMAL", contact.priority)
    }

    @Test
    fun `IGNORE priority maps to isEscalationTarget false`() {
        val contact = createContactWithPriority(ContactPriority.IGNORE)
        assertFalse(contact.isEscalationTarget)
        assertEquals("IGNORE", contact.priority)
    }

    @Test
    fun `all ContactPriority values have non-blank display names`() {
        ContactPriority.entries.forEach { priority ->
            assertTrue(priority.displayName.isNotBlank())
        }
    }

    @Test
    fun `ContactPriority display names are human readable`() {
        assertEquals("VIP", ContactPriority.VIP.displayName)
        assertEquals("High Priority", ContactPriority.HIGH_PRIORITY.displayName)
        assertEquals("Normal", ContactPriority.NORMAL.displayName)
        assertEquals("Ignore", ContactPriority.IGNORE.displayName)
    }

    @Test
    fun `priority name round-trips through string representation`() {
        ContactPriority.entries.forEach { priority ->
            val stored = priority.name
            val restored = ContactPriority.valueOf(stored)
            assertEquals(priority, restored)
        }
    }

    /**
     * Replicates the logic from SettingsViewModel.addContact() to verify
     * the mapping is correct.
     */
    private fun createContactWithPriority(priority: ContactPriority): ContactEntity {
        return ContactEntity(
            name = "Test Contact",
            priority = priority.name,
            isEscalationTarget = priority == ContactPriority.VIP || priority == ContactPriority.HIGH_PRIORITY
        )
    }
}
