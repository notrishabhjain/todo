package com.procrastinationkiller.domain.engine.insights

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecommendationEngineTest {

    private lateinit var engine: RecommendationEngine

    @BeforeEach
    fun setup() {
        engine = RecommendationEngine()
    }

    @Test
    fun `generates scheduling recommendation for peak hours`() {
        val peakHours = listOf(
            HourlyProductivity(hour = 10, completionCount = 5, avgCompletionSpeedMs = 3600000)
        )

        val recommendations = engine.generateRecommendations(
            peakHours = peakHours,
            procrastinationPatterns = emptyList(),
            senderPatterns = emptyList(),
            velocityTrend = VelocityTrend(),
            streak = StreakInfo(),
            bottlenecks = emptyList()
        )

        assertTrue(recommendations.any { it.type == RecommendationType.SCHEDULE_OPTIMIZATION })
        assertTrue(recommendations.any { it.message.contains("10 AM") })
    }

    @Test
    fun `generates procrastination alert for delayed patterns`() {
        val patterns = listOf(
            ProcrastinationPattern(
                category = "LOW",
                avgDelayMs = 5L * 24 * 60 * 60 * 1000,
                count = 3
            )
        )

        val recommendations = engine.generateRecommendations(
            peakHours = emptyList(),
            procrastinationPatterns = patterns,
            senderPatterns = emptyList(),
            velocityTrend = VelocityTrend(),
            streak = StreakInfo(),
            bottlenecks = emptyList()
        )

        assertTrue(recommendations.any { it.type == RecommendationType.PROCRASTINATION_ALERT })
        assertTrue(recommendations.any { it.message.contains("LOW") })
    }

    @Test
    fun `generates sender management recommendation for slow responses`() {
        val senderPatterns = listOf(
            SenderResponsePattern(sender = "Alice", avgResponseTimeMs = 3600000, taskCount = 5),
            SenderResponsePattern(
                sender = "Bob",
                avgResponseTimeMs = 5L * 24 * 60 * 60 * 1000,
                taskCount = 3
            )
        )

        val recommendations = engine.generateRecommendations(
            peakHours = emptyList(),
            procrastinationPatterns = emptyList(),
            senderPatterns = senderPatterns,
            velocityTrend = VelocityTrend(),
            streak = StreakInfo(),
            bottlenecks = emptyList()
        )

        assertTrue(recommendations.any { it.type == RecommendationType.SENDER_MANAGEMENT })
        assertTrue(recommendations.any { it.message.contains("Bob") })
    }

    @Test
    fun `generates velocity feedback for improving trend`() {
        val velocityTrend = VelocityTrend(trend = TrendDirection.IMPROVING, slope = 0.5)

        val recommendations = engine.generateRecommendations(
            peakHours = emptyList(),
            procrastinationPatterns = emptyList(),
            senderPatterns = emptyList(),
            velocityTrend = velocityTrend,
            streak = StreakInfo(),
            bottlenecks = emptyList()
        )

        assertTrue(recommendations.any { it.type == RecommendationType.VELOCITY_FEEDBACK })
        assertTrue(recommendations.any { it.message.contains("improving") })
    }

    @Test
    fun `generates streak encouragement for active streak`() {
        val streak = StreakInfo(currentStreak = 5, longestStreak = 5)

        val recommendations = engine.generateRecommendations(
            peakHours = emptyList(),
            procrastinationPatterns = emptyList(),
            senderPatterns = emptyList(),
            velocityTrend = VelocityTrend(),
            streak = streak,
            bottlenecks = emptyList()
        )

        assertTrue(recommendations.any { it.type == RecommendationType.STREAK_ENCOURAGEMENT })
        assertTrue(recommendations.any { it.message.contains("5-day streak") })
    }

    @Test
    fun `returns empty recommendations for no data`() {
        val recommendations = engine.generateRecommendations(
            peakHours = emptyList(),
            procrastinationPatterns = emptyList(),
            senderPatterns = emptyList(),
            velocityTrend = VelocityTrend(),
            streak = StreakInfo(),
            bottlenecks = emptyList()
        )

        assertTrue(recommendations.isEmpty())
    }
}
