package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VelocityTrackerTest {

    private lateinit var tracker: VelocityTracker

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }

    @BeforeEach
    fun setup() {
        tracker = VelocityTracker()
    }

    @Test
    fun `computeVelocityTrend returns stable for empty tasks`() {
        val result = tracker.computeVelocityTrend(emptyList())
        assertEquals(TrendDirection.STABLE, result.trend)
        assertEquals(0.0, result.slope)
    }

    @Test
    fun `computeVelocityTrend detects improving trend`() {
        val now = System.currentTimeMillis()
        // Create tasks with increasing daily completions
        val tasks = mutableListOf<TaskEntity>()
        for (day in 0 until 14) {
            val completionsForDay = day + 1 // increasing
            for (i in 0 until completionsForDay) {
                tasks.add(
                    createTask(
                        id = (day * 20 + i).toLong(),
                        status = "COMPLETED",
                        completedAt = now - ((13 - day) * DAY_MS) + (i * 1000)
                    )
                )
            }
        }

        val result = tracker.computeVelocityTrend(tasks, daysBack = 14)
        assertEquals(TrendDirection.IMPROVING, result.trend)
        assertTrue(result.slope > 0)
    }

    @Test
    fun `computeVelocityTrend detects declining trend`() {
        val now = System.currentTimeMillis()
        val tasks = mutableListOf<TaskEntity>()
        for (day in 0 until 14) {
            val completionsForDay = 14 - day // decreasing
            for (i in 0 until completionsForDay) {
                tasks.add(
                    createTask(
                        id = (day * 20 + i).toLong(),
                        status = "COMPLETED",
                        completedAt = now - ((13 - day) * DAY_MS) + (i * 1000)
                    )
                )
            }
        }

        val result = tracker.computeVelocityTrend(tasks, daysBack = 14)
        assertEquals(TrendDirection.DECLINING, result.trend)
        assertTrue(result.slope < 0)
    }

    @Test
    fun `linearRegressionSlope returns zero for single value`() {
        val slope = tracker.linearRegressionSlope(listOf(5.0))
        assertEquals(0.0, slope)
    }

    @Test
    fun `linearRegressionSlope returns positive for increasing values`() {
        val slope = tracker.linearRegressionSlope(listOf(1.0, 2.0, 3.0, 4.0, 5.0))
        assertTrue(slope > 0)
    }

    @Test
    fun `linearRegressionSlope returns negative for decreasing values`() {
        val slope = tracker.linearRegressionSlope(listOf(5.0, 4.0, 3.0, 2.0, 1.0))
        assertTrue(slope < 0)
    }

    @Test
    fun `linearRegressionSlope returns near zero for constant values`() {
        val slope = tracker.linearRegressionSlope(listOf(3.0, 3.0, 3.0, 3.0))
        assertEquals(0.0, slope, 0.001)
    }

    private fun createTask(
        id: Long = 0,
        status: String = "PENDING",
        completedAt: Long? = null
    ): TaskEntity {
        return TaskEntity(
            id = id,
            title = "Task $id",
            description = "Description",
            priority = "MEDIUM",
            status = status,
            createdAt = System.currentTimeMillis() - DAY_MS * 30,
            completedAt = completedAt
        )
    }
}
