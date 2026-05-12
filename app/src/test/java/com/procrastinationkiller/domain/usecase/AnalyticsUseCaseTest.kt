package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.entity.TaskEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Calendar

class AnalyticsUseCaseTest {

    private lateinit var analyticsUseCase: AnalyticsUseCase

    @BeforeEach
    fun setup() {
        analyticsUseCase = AnalyticsUseCase(FakeTaskRepository())
    }

    @Test
    fun `computes completed today correctly`() {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            createTask(status = "COMPLETED", completedAt = now),
            createTask(status = "COMPLETED", completedAt = now - 1000),
            createTask(status = "COMPLETED", completedAt = now - 48 * 60 * 60 * 1000L),
            createTask(status = "PENDING")
        )

        val result = analyticsUseCase.computeAnalytics(tasks, TimeRange.WEEK)

        assertEquals(2, result.completedToday)
    }

    @Test
    fun `computes completion rate correctly`() {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            createTask(status = "COMPLETED", completedAt = now),
            createTask(status = "COMPLETED", completedAt = now),
            createTask(status = "PENDING"),
            createTask(status = "PENDING")
        )

        val result = analyticsUseCase.computeAnalytics(tasks, TimeRange.WEEK)

        assertEquals(0.5f, result.completionRate)
    }

    @Test
    fun `computes pending backlog`() {
        val tasks = listOf(
            createTask(status = "PENDING"),
            createTask(status = "IN_PROGRESS"),
            createTask(status = "COMPLETED", completedAt = System.currentTimeMillis()),
            createTask(status = "CANCELLED")
        )

        val result = analyticsUseCase.computeAnalytics(tasks, TimeRange.WEEK)

        assertEquals(2, result.pendingBacklog)
    }

    @Test
    fun `identifies most ignored tasks as oldest pending`() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val tasks = listOf(
            createTask(title = "Old task", status = "PENDING", createdAt = now - 10 * dayMs),
            createTask(title = "Newer task", status = "PENDING", createdAt = now - dayMs),
            createTask(title = "Done task", status = "COMPLETED", createdAt = now - 15 * dayMs, completedAt = now)
        )

        val result = analyticsUseCase.computeAnalytics(tasks, TimeRange.WEEK)

        assertTrue(result.mostIgnoredTasks.isNotEmpty())
        assertEquals("Old task", result.mostIgnoredTasks.first().title)
    }

    @Test
    fun `empty tasks returns zero analytics`() {
        val result = analyticsUseCase.computeAnalytics(emptyList(), TimeRange.WEEK)

        assertEquals(0, result.completedToday)
        assertEquals(0, result.completedThisWeek)
        assertEquals(0, result.completedThisMonth)
        assertEquals(0, result.pendingBacklog)
        assertEquals(0f, result.completionRate)
        assertTrue(result.mostIgnoredTasks.isEmpty())
    }

    @Test
    fun `productivity trend returns data points for week`() {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            createTask(status = "COMPLETED", completedAt = now, createdAt = now - 1000)
        )

        val result = analyticsUseCase.computeAnalytics(tasks, TimeRange.WEEK)

        assertEquals(7, result.productivityTrend.size)
    }

    @Test
    fun `productivity trend returns data points for month`() {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            createTask(status = "COMPLETED", completedAt = now, createdAt = now - 1000)
        )

        val result = analyticsUseCase.computeAnalytics(tasks, TimeRange.MONTH)

        assertEquals(30, result.productivityTrend.size)
    }

    @Test
    fun `computes completed this week`() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val weekStart = cal.timeInMillis

        val tasks = listOf(
            createTask(status = "COMPLETED", completedAt = weekStart + 1000),
            createTask(status = "COMPLETED", completedAt = weekStart - 1000)
        )

        val result = analyticsUseCase.computeAnalytics(tasks, TimeRange.WEEK)

        assertEquals(1, result.completedThisWeek)
    }

    private fun createTask(
        title: String = "Test Task",
        status: String = "PENDING",
        priority: String = "MEDIUM",
        createdAt: Long = System.currentTimeMillis(),
        completedAt: Long? = null
    ) = TaskEntity(
        title = title,
        description = "",
        priority = priority,
        status = status,
        createdAt = createdAt,
        completedAt = completedAt
    )
}
