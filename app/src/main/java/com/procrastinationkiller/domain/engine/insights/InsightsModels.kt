package com.procrastinationkiller.domain.engine.insights

data class ProductivityInsights(
    val productivityScore: ProductivityScore = ProductivityScore(),
    val peakHours: List<HourlyProductivity> = emptyList(),
    val procrastinationPatterns: List<ProcrastinationPattern> = emptyList(),
    val senderResponsePatterns: List<SenderResponsePattern> = emptyList(),
    val velocityTrend: VelocityTrend = VelocityTrend(),
    val recommendations: List<Recommendation> = emptyList(),
    val bottlenecks: List<Bottleneck> = emptyList(),
    val gamification: GamificationState = GamificationState()
)

data class HourlyProductivity(
    val hour: Int,
    val completionCount: Int,
    val avgCompletionSpeedMs: Long
)

data class ProcrastinationPattern(
    val category: String,
    val avgDelayMs: Long,
    val count: Int
)

data class SenderResponsePattern(
    val sender: String,
    val avgResponseTimeMs: Long,
    val taskCount: Int
)

data class VelocityTrend(
    val dailyCompletions: List<DailyCompletion> = emptyList(),
    val slope: Double = 0.0,
    val trend: TrendDirection = TrendDirection.STABLE
)

data class DailyCompletion(
    val dayTimestamp: Long,
    val count: Int
)

enum class TrendDirection {
    IMPROVING,
    DECLINING,
    STABLE
}

data class Recommendation(
    val type: RecommendationType,
    val message: String,
    val priority: Int = 0
)

enum class RecommendationType {
    SCHEDULE_OPTIMIZATION,
    PROCRASTINATION_ALERT,
    SENDER_MANAGEMENT,
    VELOCITY_FEEDBACK,
    STREAK_ENCOURAGEMENT,
    BOTTLENECK_RESOLUTION
}

data class ProductivityScore(
    val overall: Int = 0,
    val completionRate: Float = 0f,
    val velocityScore: Float = 0f,
    val responseScore: Float = 0f,
    val backlogScore: Float = 0f
)

data class Bottleneck(
    val type: BottleneckType,
    val description: String,
    val affectedCount: Int,
    val severity: Int
)

enum class BottleneckType {
    STALE_TASKS,
    UNRESPONSIVE_SENDER,
    INACTIVE_PERIOD,
    OVERDUE_TASKS
}

data class Achievement(
    val type: String,
    val title: String,
    val description: String,
    val unlockedAt: Long? = null,
    val isUnlocked: Boolean = false
)

data class StreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDay: Long = 0L
)

data class GamificationState(
    val streak: StreakInfo = StreakInfo(),
    val achievements: List<Achievement> = emptyList(),
    val nextGoal: Achievement? = null,
    val totalTasksCompleted: Int = 0
)
