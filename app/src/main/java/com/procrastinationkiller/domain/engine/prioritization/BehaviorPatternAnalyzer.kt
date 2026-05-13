package com.procrastinationkiller.domain.engine.prioritization

import com.procrastinationkiller.data.local.dao.BehaviorPatternDao
import com.procrastinationkiller.data.local.entity.BehaviorPatternEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviorPatternAnalyzer @Inject constructor(
    private val behaviorPatternDao: BehaviorPatternDao
) {

    companion object {
        private const val HIGH_IGNORE_RATE_THRESHOLD = 0.6f
        private const val HIGH_COMPLETION_RATE_THRESHOLD = 0.7f
        private const val MIN_INTERACTIONS = 3
    }

    suspend fun analyze(
        sender: String,
        sourceApp: String
    ): BehaviorPatternResult {
        val factors = mutableListOf<String>()
        var priorityModifier = 0f

        // Analyze sender patterns
        val senderPatterns = behaviorPatternDao.getBySender(sender)
        if (senderPatterns.isNotEmpty()) {
            val senderModifier = analyzeSenderPatterns(senderPatterns)
            if (senderModifier != 0f) {
                priorityModifier += senderModifier
                if (senderModifier > 0f) {
                    factors.add("SENDER_HIGH_COMPLETION_RATE")
                } else {
                    factors.add("SENDER_HIGH_IGNORE_RATE")
                }
            }
        }

        // Analyze source app patterns
        val appPatterns = behaviorPatternDao.getBySourceApp(sourceApp)
        if (appPatterns.isNotEmpty()) {
            val appModifier = analyzeAppPatterns(appPatterns)
            if (appModifier != 0f) {
                priorityModifier += appModifier
                if (appModifier > 0f) {
                    factors.add("APP_HIGH_COMPLETION_RATE")
                } else {
                    factors.add("APP_HIGH_IGNORE_RATE")
                }
            }
        }

        return BehaviorPatternResult(
            priorityModifier = priorityModifier.coerceIn(-0.3f, 0.3f),
            factors = factors
        )
    }

    private fun analyzeSenderPatterns(patterns: List<BehaviorPatternEntity>): Float {
        val totalInteractions = patterns.sumOf { it.completionCount + it.ignoreCount }
        if (totalInteractions < MIN_INTERACTIONS) return 0f

        val totalIgnored = patterns.sumOf { it.ignoreCount }
        val totalCompleted = patterns.sumOf { it.completionCount }
        val ignoreRate = totalIgnored.toFloat() / totalInteractions
        val completionRate = totalCompleted.toFloat() / totalInteractions

        return when {
            ignoreRate >= HIGH_IGNORE_RATE_THRESHOLD -> -0.2f
            completionRate >= HIGH_COMPLETION_RATE_THRESHOLD -> 0.15f
            else -> 0f
        }
    }

    private fun analyzeAppPatterns(patterns: List<BehaviorPatternEntity>): Float {
        val totalInteractions = patterns.sumOf { it.completionCount + it.ignoreCount }
        if (totalInteractions < MIN_INTERACTIONS) return 0f

        val totalIgnored = patterns.sumOf { it.ignoreCount }
        val totalCompleted = patterns.sumOf { it.completionCount }
        val ignoreRate = totalIgnored.toFloat() / totalInteractions
        val completionRate = totalCompleted.toFloat() / totalInteractions

        return when {
            ignoreRate >= HIGH_IGNORE_RATE_THRESHOLD -> -0.15f
            completionRate >= HIGH_COMPLETION_RATE_THRESHOLD -> 0.1f
            else -> 0f
        }
    }
}

data class BehaviorPatternResult(
    val priorityModifier: Float,
    val factors: List<String>
)
