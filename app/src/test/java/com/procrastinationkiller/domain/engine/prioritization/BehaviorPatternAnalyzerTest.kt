package com.procrastinationkiller.domain.engine.prioritization

import com.procrastinationkiller.data.local.entity.BehaviorPatternEntity
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BehaviorPatternAnalyzerTest {

    private lateinit var analyzer: BehaviorPatternAnalyzer
    private lateinit var fakeDao: FakeBehaviorPatternDao

    @BeforeEach
    fun setup() {
        fakeDao = FakeBehaviorPatternDao()
        analyzer = BehaviorPatternAnalyzer(fakeDao)
    }

    @Test
    fun `high ignore rate sender gets negative priority modifier`() = runBlocking {
        fakeDao.patterns.add(
            BehaviorPatternEntity(
                id = 1,
                sender = "spammer",
                sourceApp = "com.whatsapp",
                taskType = "general",
                avgCompletionTimeMs = 0,
                completionCount = 1,
                ignoreCount = 10,
                lastUpdated = System.currentTimeMillis()
            )
        )

        val result = analyzer.analyze(sender = "spammer", sourceApp = "com.other")

        assertTrue(result.priorityModifier < 0f)
        assertTrue(result.factors.contains("SENDER_HIGH_IGNORE_RATE"))
    }

    @Test
    fun `high completion rate sender gets positive priority modifier`() = runBlocking {
        fakeDao.patterns.add(
            BehaviorPatternEntity(
                id = 1,
                sender = "boss",
                sourceApp = "com.slack",
                taskType = "report",
                avgCompletionTimeMs = 30000,
                completionCount = 15,
                ignoreCount = 1,
                lastUpdated = System.currentTimeMillis()
            )
        )

        val result = analyzer.analyze(sender = "boss", sourceApp = "com.other")

        assertTrue(result.priorityModifier > 0f)
        assertTrue(result.factors.contains("SENDER_HIGH_COMPLETION_RATE"))
    }

    @Test
    fun `unknown sender returns neutral modifier`() = runBlocking {
        val result = analyzer.analyze(sender = "unknown_person", sourceApp = "com.unknown")

        assertEquals(0f, result.priorityModifier)
        assertTrue(result.factors.isEmpty())
    }

    @Test
    fun `sender with too few interactions returns neutral`() = runBlocking {
        fakeDao.patterns.add(
            BehaviorPatternEntity(
                id = 1,
                sender = "new_person",
                sourceApp = "com.whatsapp",
                taskType = "general",
                avgCompletionTimeMs = 0,
                completionCount = 1,
                ignoreCount = 1,
                lastUpdated = System.currentTimeMillis()
            )
        )

        val result = analyzer.analyze(sender = "new_person", sourceApp = "com.other")

        assertEquals(0f, result.priorityModifier)
    }

    @Test
    fun `high ignore rate app gets negative modifier`() = runBlocking {
        fakeDao.patterns.add(
            BehaviorPatternEntity(
                id = 1,
                sender = "someone",
                sourceApp = "com.spam.app",
                taskType = "notification",
                avgCompletionTimeMs = 0,
                completionCount = 1,
                ignoreCount = 8,
                lastUpdated = System.currentTimeMillis()
            )
        )

        val result = analyzer.analyze(sender = "other_person", sourceApp = "com.spam.app")

        assertTrue(result.priorityModifier < 0f)
        assertTrue(result.factors.contains("APP_HIGH_IGNORE_RATE"))
    }

    @Test
    fun `priority modifier is clamped to valid range`() = runBlocking {
        // Add many patterns for both sender and app with high ignore
        fakeDao.patterns.addAll(
            listOf(
                BehaviorPatternEntity(
                    id = 1,
                    sender = "bad_sender",
                    sourceApp = "com.bad.app",
                    taskType = "spam",
                    avgCompletionTimeMs = 0,
                    completionCount = 0,
                    ignoreCount = 50,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        )

        val result = analyzer.analyze(sender = "bad_sender", sourceApp = "com.bad.app")

        assertTrue(result.priorityModifier >= -0.3f)
        assertTrue(result.priorityModifier <= 0.3f)
    }
}
