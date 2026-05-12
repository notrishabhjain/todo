package com.procrastinationkiller.domain.engine.transcript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConversationContextTest {

    private lateinit var builder: ConversationContextBuilder

    @BeforeEach
    fun setup() {
        builder = ConversationContextBuilder()
    }

    @Test
    fun `standup transcript detected as STANDUP`() {
        val transcript = """
            Alice: What did you do yesterday?
            Bob: I worked on the login page. Today I will fix the blocker
            Charlie: I'm stuck on the API issue, it's a blocker
        """.trimIndent()

        val context = builder.build(transcript)

        assertEquals(MeetingType.STANDUP, context.meetingType)
    }

    @Test
    fun `planning keywords detected as PLANNING`() {
        val transcript = """
            PM: Let's estimate the sprint backlog
            Dev: This story points estimate is 5
            PM: What's our velocity this iteration?
        """.trimIndent()

        val context = builder.build(transcript)

        assertEquals(MeetingType.PLANNING, context.meetingType)
    }

    @Test
    fun `review keywords detected as REVIEW`() {
        val transcript = """
            Alice: Let me demo the new feature
            Bob: Looks good, I approve
            Charlie: I have some feedback on the PR review
        """.trimIndent()

        val context = builder.build(transcript)

        assertEquals(MeetingType.REVIEW, context.meetingType)
    }

    @Test
    fun `general conversation has GENERAL type`() {
        val transcript = """
            Alice: How are you doing?
            Bob: Good, thanks
            Charlie: Let's grab lunch
        """.trimIndent()

        val context = builder.build(transcript)

        assertEquals(MeetingType.GENERAL, context.meetingType)
    }

    @Test
    fun `participants extracted from speaker lines`() {
        val transcript = """
            Alice: Hello
            Bob: Hi there
            Charlie: Hey everyone
        """.trimIndent()

        val context = builder.build(transcript)

        assertEquals(3, context.participants.size)
        assertTrue(context.participants.contains("Alice"))
        assertTrue(context.participants.contains("Bob"))
        assertTrue(context.participants.contains("Charlie"))
    }

    @Test
    fun `empty transcript produces empty context`() {
        val context = builder.build("")

        assertEquals(MeetingType.GENERAL, context.meetingType)
        assertTrue(context.participants.isEmpty())
    }

    @Test
    fun `topic extracted from discuss pattern`() {
        val transcript = """
            Alice: Let's discuss the deployment pipeline
            Bob: Sure, sounds good
        """.trimIndent()

        val context = builder.build(transcript)

        assertTrue(context.currentTopic?.contains("deployment") == true)
    }
}
