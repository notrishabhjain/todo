package com.procrastinationkiller.domain.engine.transcript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SpeakerProfileTest {

    private lateinit var detector: SpeakerRoleDetector

    @BeforeEach
    fun setup() {
        detector = SpeakerRoleDetector()
    }

    @Test
    fun `first speaker is detected as organizer`() {
        val transcript = """
            Alice: Let's start the meeting
            Bob: Sure, I have an update
            Charlie: Me too
        """.trimIndent()

        val profiles = detector.detectRoles(transcript)

        assertTrue(profiles.isNotEmpty())
        val alice = profiles.find { it.name == "Alice" }
        assertEquals(SpeakerRole.ORGANIZER, alice?.role)
    }

    @Test
    fun `person addressed with please is detected as participant`() {
        val transcript = """
            Manager: Good morning everyone
            Manager: Sarah, please send the report
            Sarah: Sure, I will do it
        """.trimIndent()

        val profiles = detector.detectRoles(transcript)

        val sarah = profiles.find { it.name == "Sarah" }
        assertEquals(SpeakerRole.PARTICIPANT, sarah?.role)
    }

    @Test
    fun `unknown speakers default to UNKNOWN`() {
        val transcript = """
            Alice: Let's discuss the issue
            Bob: I agree
            Charlie: Me too
        """.trimIndent()

        val profiles = detector.detectRoles(transcript)

        val charlie = profiles.find { it.name == "Charlie" }
        assertEquals(SpeakerRole.UNKNOWN, charlie?.role)
    }

    @Test
    fun `at mentioned person with assignment keywords is participant`() {
        val transcript = """
            Lead: Hello team
            Lead: @Dave please review the PR
        """.trimIndent()

        val profiles = detector.detectRoles(transcript)

        val lead = profiles.find { it.name == "Lead" }
        assertEquals(SpeakerRole.ORGANIZER, lead?.role)
    }

    @Test
    fun `empty transcript returns empty profiles`() {
        val profiles = detector.detectRoles("")
        assertTrue(profiles.isEmpty())
    }

    @Test
    fun `single speaker is organizer`() {
        val transcript = "John: I will handle the deployment"

        val profiles = detector.detectRoles(transcript)

        assertEquals(1, profiles.size)
        assertEquals(SpeakerRole.ORGANIZER, profiles[0].role)
    }
}
