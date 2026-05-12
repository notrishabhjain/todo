package com.procrastinationkiller.domain.engine.prioritization

import com.procrastinationkiller.data.local.dao.BehaviorPatternDao
import com.procrastinationkiller.data.local.entity.BehaviorPatternEntity
import com.procrastinationkiller.domain.model.TaskPriority
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SmartPrioritizationEngineTest {

    private lateinit var engine: SmartPrioritizationEngine
    private lateinit var fakeDao: FakeBehaviorPatternDao
    private lateinit var behaviorAnalyzer: BehaviorPatternAnalyzer
    private lateinit var timeOfDayAnalyzer: TimeOfDayAnalyzer
    private lateinit var deadlineEscalator: DeadlineProximityEscalator
    private lateinit var contextualAnalyzer: ContextualSignalAnalyzer

    @BeforeEach
    fun setup() {
        fakeDao = FakeBehaviorPatternDao()
        behaviorAnalyzer = BehaviorPatternAnalyzer(fakeDao)
        timeOfDayAnalyzer = TimeOfDayAnalyzer()
        deadlineEscalator = DeadlineProximityEscalator()
        contextualAnalyzer = ContextualSignalAnalyzer()
        engine = SmartPrioritizationEngine(
            behaviorAnalyzer,
            timeOfDayAnalyzer,
            deadlineEscalator,
            contextualAnalyzer
        )
    }

    @Test
    fun `deadline within 1 hour escalates to CRITICAL`() = runBlocking {
        val deadline = System.currentTimeMillis() + 30 * 60 * 1000 // 30 minutes from now

        val result = engine.evaluate(
            text = "Send the report",
            sender = "Boss",
            sourceApp = "com.whatsapp",
            currentPriority = TaskPriority.MEDIUM,
            deadlineMs = deadline
        )

        assertEquals(TaskPriority.CRITICAL, result.priority)
        assertTrue(result.shouldEscalate)
        assertTrue(result.contributingFactors.contains("DEADLINE_WITHIN_1_HOUR"))
    }

    @Test
    fun `sender importance boosts priority based on behavior patterns`() = runBlocking {
        // Add high-completion patterns for sender
        fakeDao.patterns.add(
            BehaviorPatternEntity(
                id = 1,
                sender = "important_boss",
                sourceApp = "com.slack",
                taskType = "report",
                avgCompletionTimeMs = 60000,
                completionCount = 20,
                ignoreCount = 0,
                lastUpdated = System.currentTimeMillis()
            )
        )

        val result = engine.evaluate(
            text = "Please review this",
            sender = "important_boss",
            sourceApp = "com.slack",
            currentPriority = TaskPriority.MEDIUM
        )

        assertTrue(result.contributingFactors.contains("SENDER_HIGH_COMPLETION_RATE"))
    }

    @Test
    fun `contextual signals affect urgency score`() = runBlocking {
        val result = engine.evaluate(
            text = "URGENT!!! SEND THE REPORT NOW!!!",
            sender = "Manager",
            sourceApp = "com.whatsapp",
            currentPriority = TaskPriority.MEDIUM
        )

        assertTrue(result.urgencyScore > 0.5f)
        assertTrue(result.contributingFactors.contains("ALL_CAPS_DETECTED"))
        assertTrue(result.contributingFactors.contains("EXCLAMATION_MARKS"))
    }

    @Test
    fun `no escalation for low-urgency message`() = runBlocking {
        val result = engine.evaluate(
            text = "can you check the logs when free",
            sender = "Colleague",
            sourceApp = "com.slack",
            currentPriority = TaskPriority.LOW
        )

        assertEquals(TaskPriority.LOW, result.priority)
    }

    @Test
    fun `repeated messages from sender increase urgency`() = runBlocking {
        val result = engine.evaluate(
            text = "Hey, did you see my message?",
            sender = "Boss",
            sourceApp = "com.whatsapp",
            currentPriority = TaskPriority.MEDIUM,
            recentMessagesFromSender = 4
        )

        assertTrue(result.contributingFactors.contains("REPEATED_MESSAGES"))
        assertTrue(result.urgencyScore > 0.3f)
    }

    @Test
    fun `combines multiple signals for higher urgency`() = runBlocking {
        val deadline = System.currentTimeMillis() + 3 * 60 * 60 * 1000 // 3 hours

        val result = engine.evaluate(
            text = "SEND IT NOW!!!",
            sender = "Boss",
            sourceApp = "com.whatsapp",
            currentPriority = TaskPriority.MEDIUM,
            deadlineMs = deadline,
            recentMessagesFromSender = 3
        )

        assertTrue(result.urgencyScore > 0.6f)
        assertTrue(result.shouldEscalate)
    }
}
