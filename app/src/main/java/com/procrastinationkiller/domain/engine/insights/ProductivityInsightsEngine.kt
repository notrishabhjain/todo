package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductivityInsightsEngine @Inject constructor(
    private val peakProductivityDetector: PeakProductivityDetector,
    private val procrastinationPatternDetector: ProcrastinationPatternDetector,
    private val senderResponseAnalyzer: SenderResponseAnalyzer,
    private val velocityTracker: VelocityTracker,
    private val recommendationEngine: RecommendationEngine,
    private val productivityScoreCalculator: ProductivityScoreCalculator,
    private val bottleneckDetector: BottleneckDetector,
    private val gamificationEngine: GamificationEngine
) {

    fun computeInsights(
        tasks: List<TaskEntity>,
        suggestions: List<TaskSuggestionEntity> = emptyList()
    ): ProductivityInsights {
        val peakHours = peakProductivityDetector.getTopPeakHours(tasks)
        val procrastinationPatterns = procrastinationPatternDetector.detectPatterns(tasks) +
            procrastinationPatternDetector.detectPendingPatterns(tasks)
        val senderPatterns = senderResponseAnalyzer.analyzeResponsePatterns(tasks, suggestions)
        val velocityTrend = velocityTracker.computeVelocityTrend(tasks)
        val bottlenecks = bottleneckDetector.detectBottlenecks(tasks)
        val gamification = gamificationEngine.computeGamificationState(tasks)
        val productivityScore = productivityScoreCalculator.calculateScore(
            tasks, velocityTrend, senderPatterns
        )

        val recommendations = recommendationEngine.generateRecommendations(
            peakHours = peakHours,
            procrastinationPatterns = procrastinationPatterns,
            senderPatterns = senderPatterns,
            velocityTrend = velocityTrend,
            streak = gamification.streak,
            bottlenecks = bottlenecks
        )

        return ProductivityInsights(
            productivityScore = productivityScore,
            peakHours = peakHours,
            procrastinationPatterns = procrastinationPatterns,
            senderResponsePatterns = senderPatterns,
            velocityTrend = velocityTrend,
            recommendations = recommendations,
            bottlenecks = bottlenecks,
            gamification = gamification
        )
    }
}
