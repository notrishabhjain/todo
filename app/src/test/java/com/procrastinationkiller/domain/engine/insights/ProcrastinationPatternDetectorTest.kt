package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProcrastinationPatternDetectorTest {

    private lateinit var detector: ProcrastinationPatternDetector

    @BeforeEach
    fun setup() {
        detector = ProcrastinationPatternDetector()
    }

    @Test
    fun `detectPatterns returns empty for no tasks`() {
        val result = detector.detectPatterns(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectPatterns returns empty for tasks completed quickly`() {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            createTask(
                id = 1,
                status = "COMPLETED",
                createdAt = now - 3600000, // 1 hour ago
                completedAt = now
            )
        )
        val result = detector.detectPatterns(tasks)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectPatterns flags tasks delayed more than 3 days`() {
        val now = System.currentTimeMillis()
        val fourDaysMs = 4L * 24 * 60 * 60 * 1000

        val tasks = listOf(
            createTask(
                id = 1,
                status = "COMPLETED",
                priority = "LOW",
                createdAt = now - fourDaysMs,
                completedAt = now
            ),
            createTask(
                id = 2,
                status = "COMPLETED",
                priority = "LOW",
                createdAt = now - fourDaysMs - 86400000,
                completedAt = now
            )
        )

        val result = detector.detectPatterns(tasks)
        assertTrue(result.isNotEmpty())
        assertEquals("LOW", result.first().category)
        assertEquals(2, result.first().count)
    }

    @Test
    fun `detectPatterns does not flag fast tasks`() {
        val now = System.currentTimeMillis()
        val oneHourMs = 60 * 60 * 1000L

        val tasks = listOf(
            createTask(
                id = 1,
                status = "COMPLETED",
                priority = "HIGH",
                createdAt = now - oneHourMs,
                completedAt = now
            )
        )

        val result = detector.detectPatterns(tasks)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectPendingPatterns identifies stale pending tasks`() {
        val now = System.currentTimeMillis()
        val fiveDaysMs = 5L * 24 * 60 * 60 * 1000

        val tasks = listOf(
            createTask(
                id = 1,
                status = "PENDING",
                priority = "MEDIUM",
                createdAt = now - fiveDaysMs
            )
        )

        val result = detector.detectPendingPatterns(tasks)
        assertTrue(result.isNotEmpty())
        assertEquals("PENDING_MEDIUM", result.first().category)
    }

    @Test
    fun `detectPendingPatterns returns empty for recent tasks`() {
        val now = System.currentTimeMillis()
        val oneHourMs = 60 * 60 * 1000L

        val tasks = listOf(
            createTask(
                id = 1,
                status = "PENDING",
                priority = "HIGH",
                createdAt = now - oneHourMs
            )
        )

        val result = detector.detectPendingPatterns(tasks)
        assertTrue(result.isEmpty())
    }

    private fun createTask(
        id: Long = 0,
        status: String = "PENDING",
        priority: String = "MEDIUM",
        createdAt: Long = System.currentTimeMillis(),
        completedAt: Long? = null
    ): TaskEntity {
        return TaskEntity(
            id = id,
            title = "Task $id",
            description = "Description",
            priority = priority,
            status = status,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }
}
