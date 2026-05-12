package com.procrastinationkiller.domain.engine.insights

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationEngine @Inject constructor() {

    fun generateRecommendations(
        peakHours: List<HourlyProductivity>,
        procrastinationPatterns: List<ProcrastinationPattern>,
        senderPatterns: List<SenderResponsePattern>,
        velocityTrend: VelocityTrend,
        streak: StreakInfo,
        bottlenecks: List<Bottleneck>
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        generatePeakHourRecommendations(peakHours, recommendations)
        generateProcrastinationRecommendations(procrastinationPatterns, recommendations)
        generateSenderRecommendations(senderPatterns, recommendations)
        generateVelocityRecommendations(velocityTrend, recommendations)
        generateStreakRecommendations(streak, recommendations)
        generateBottleneckRecommendations(bottlenecks, recommendations)

        return recommendations.sortedByDescending { it.priority }
    }

    private fun generatePeakHourRecommendations(
        peakHours: List<HourlyProductivity>,
        recommendations: MutableList<Recommendation>
    ) {
        if (peakHours.isNotEmpty()) {
            val topHour = peakHours.first()
            val hourStr = formatHour(topHour.hour)
            recommendations.add(
                Recommendation(
                    type = RecommendationType.SCHEDULE_OPTIMIZATION,
                    message = "You are most productive at $hourStr - schedule important tasks then.",
                    priority = 8
                )
            )
        }
    }

    private fun generateProcrastinationRecommendations(
        patterns: List<ProcrastinationPattern>,
        recommendations: MutableList<Recommendation>
    ) {
        if (patterns.isNotEmpty()) {
            val worstPattern = patterns.first()
            val daysDelayed = worstPattern.avgDelayMs / (24 * 60 * 60 * 1000L)
            recommendations.add(
                Recommendation(
                    type = RecommendationType.PROCRASTINATION_ALERT,
                    message = "You tend to delay ${worstPattern.category} tasks by $daysDelayed days on average. Try breaking them into smaller steps.",
                    priority = 9
                )
            )
        }
    }

    private fun generateSenderRecommendations(
        patterns: List<SenderResponsePattern>,
        recommendations: MutableList<Recommendation>
    ) {
        if (patterns.size >= 2) {
            val slowest = patterns.last()
            val daysToRespond = slowest.avgResponseTimeMs / (24 * 60 * 60 * 1000L)
            if (daysToRespond > 2) {
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.SENDER_MANAGEMENT,
                        message = "Tasks from ${slowest.sender} take ${daysToRespond} days on average. Consider setting reminders for their messages.",
                        priority = 6
                    )
                )
            }
        }
    }

    private fun generateVelocityRecommendations(
        velocityTrend: VelocityTrend,
        recommendations: MutableList<Recommendation>
    ) {
        when (velocityTrend.trend) {
            TrendDirection.IMPROVING -> {
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.VELOCITY_FEEDBACK,
                        message = "Your completion rate is improving! Keep up the momentum.",
                        priority = 5
                    )
                )
            }
            TrendDirection.DECLINING -> {
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.VELOCITY_FEEDBACK,
                        message = "Your completion rate has been declining. Try focusing on fewer tasks at a time.",
                        priority = 7
                    )
                )
            }
            TrendDirection.STABLE -> {}
        }
    }

    private fun generateStreakRecommendations(
        streak: StreakInfo,
        recommendations: MutableList<Recommendation>
    ) {
        if (streak.currentStreak >= 3) {
            recommendations.add(
                Recommendation(
                    type = RecommendationType.STREAK_ENCOURAGEMENT,
                    message = "You are on a ${streak.currentStreak}-day streak! Complete a task today to keep it going.",
                    priority = 4
                )
            )
        }
    }

    private fun generateBottleneckRecommendations(
        bottlenecks: List<Bottleneck>,
        recommendations: MutableList<Recommendation>
    ) {
        bottlenecks.firstOrNull()?.let { bottleneck ->
            recommendations.add(
                Recommendation(
                    type = RecommendationType.BOTTLENECK_RESOLUTION,
                    message = bottleneck.description,
                    priority = bottleneck.severity
                )
            )
        }
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }
}
