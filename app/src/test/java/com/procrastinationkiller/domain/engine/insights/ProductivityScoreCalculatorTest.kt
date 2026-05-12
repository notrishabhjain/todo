package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProductivityScoreCalculatorTest {

    private lateinit var calculator: ProductivityScoreCalculator

    @BeforeEach
    fun setup() {
        calculator = ProductivityScoreCalculator()
    }

    @Test
    fun `calculateScore returns baseline score for empty tasks`() {
        val result = calculator.calculateScore(
            emptyList(),
            VelocityTrend(),
            emptyList()
        )
        // With no tasks: completion=0, velocity=50 (stable), response=50 (default), backlog=100 (no pending)
        // Score = 0*0.35 + 50*0.25 + 50*0.20 + 100*0.20 = 42
        assertEquals(42, result.overall)
    }

    @Test
    fun `calculateScore returns high score for all completed tasks`() {
        val tasks = (1..10L).map { id ->
            TaskEntity(
                id = id,
                title = "Task $id",
                status = "COMPLETED",
                createdAt = System.currentTimeMillis() - 86400000,
                completedAt = System.currentTimeMillis()
            )
        }

        val velocityTrend = VelocityTrend(trend = TrendDirection.IMPROVING, slope = 1.0)

        val result = calculator.calculateScore(tasks, velocityTrend, emptyList())
        assertTrue(result.overall >= 70)
    }

    @Test
    fun `calculateScore returns low score for all ignored tasks`() {
        val tasks = (1..10L).map { id ->
            TaskEntity(
                id = id,
                title = "Task $id",
                status = "PENDING",
                createdAt = System.currentTimeMillis() - 86400000 * 7
            )
        }

        val velocityTrend = VelocityTrend(trend = TrendDirection.DECLINING, slope = -1.0)

        val result = calculator.calculateScore(tasks, velocityTrend, emptyList())
        assertTrue(result.overall <= 30)
    }

    @Test
    fun `calculateScore produces proportional score for mixed tasks`() {
        val tasks = (1..10L).map { id ->
            if (id <= 5) {
                TaskEntity(
                    id = id,
                    title = "Task $id",
                    status = "COMPLETED",
                    createdAt = System.currentTimeMillis() - 86400000,
                    completedAt = System.currentTimeMillis()
                )
            } else {
                TaskEntity(
                    id = id,
                    title = "Task $id",
                    status = "PENDING",
                    createdAt = System.currentTimeMillis() - 86400000
                )
            }
        }

        val velocityTrend = VelocityTrend(trend = TrendDirection.STABLE, slope = 0.0)

        val result = calculator.calculateScore(tasks, velocityTrend, emptyList())
        assertTrue(result.overall in 30..70)
    }

    @Test
    fun `computeCompletionRate returns 100 for all completed`() {
        val tasks = (1..5L).map {
            TaskEntity(id = it, title = "Task $it", status = "COMPLETED", completedAt = System.currentTimeMillis())
        }
        val rate = calculator.computeCompletionRate(tasks)
        assertEquals(100f, rate)
    }

    @Test
    fun `computeCompletionRate returns 0 for no completed`() {
        val tasks = (1..5L).map {
            TaskEntity(id = it, title = "Task $it", status = "PENDING")
        }
        val rate = calculator.computeCompletionRate(tasks)
        assertEquals(0f, rate)
    }

    @Test
    fun `computeVelocityScore returns high for improving trend`() {
        val score = calculator.computeVelocityScore(
            VelocityTrend(trend = TrendDirection.IMPROVING, slope = 1.0)
        )
        assertTrue(score >= 70f)
    }

    @Test
    fun `computeVelocityScore returns low for declining trend`() {
        val score = calculator.computeVelocityScore(
            VelocityTrend(trend = TrendDirection.DECLINING, slope = -1.0)
        )
        assertTrue(score <= 30f)
    }

    @Test
    fun `computeBacklogScore returns 100 for no pending tasks`() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "Task 1", status = "COMPLETED", completedAt = System.currentTimeMillis())
        )
        val score = calculator.computeBacklogScore(tasks)
        assertEquals(100f, score)
    }

    @Test
    fun `computeBacklogScore returns 0 for all pending tasks`() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "Task 1", status = "PENDING")
        )
        val score = calculator.computeBacklogScore(tasks)
        assertEquals(0f, score)
    }
}
