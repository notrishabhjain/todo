package com.procrastinationkiller.domain.engine.prioritization

import com.procrastinationkiller.domain.model.TaskPriority
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartPrioritizationEngine @Inject constructor(
    private val behaviorPatternAnalyzer: BehaviorPatternAnalyzer,
    private val timeOfDayAnalyzer: TimeOfDayAnalyzer,
    private val deadlineProximityEscalator: DeadlineProximityEscalator,
    private val contextualSignalAnalyzer: ContextualSignalAnalyzer
) {

    companion object {
        private const val ESCALATION_THRESHOLD = 0.7f
    }

    suspend fun evaluate(
        text: String,
        sender: String,
        sourceApp: String,
        currentPriority: TaskPriority,
        deadlineMs: Long? = null,
        timestampMs: Long = System.currentTimeMillis(),
        recentMessagesFromSender: Int = 0
    ): PrioritizationResult {
        val contributingFactors = mutableListOf<String>()
        var urgencyScore = priorityToBaseScore(currentPriority)

        // 1. Behavior pattern analysis
        val behaviorResult = behaviorPatternAnalyzer.analyze(sender, sourceApp)
        urgencyScore += behaviorResult.priorityModifier
        contributingFactors.addAll(behaviorResult.factors)

        // 2. Time-of-day analysis
        val timeResult = timeOfDayAnalyzer.analyze(timestampMs)
        urgencyScore += timeResult.urgencyBoost
        if (timeResult.factor != null) {
            contributingFactors.add(timeResult.factor)
        }

        // 3. Deadline proximity escalation
        val deadlineResult = deadlineProximityEscalator.evaluate(deadlineMs, currentPriority)
        if (deadlineResult != null) {
            urgencyScore += deadlineResult.urgencyMultiplier
            contributingFactors.add(deadlineResult.reason)
        }

        // 4. Contextual signal analysis (tone, caps, exclamation)
        val contextResult = contextualSignalAnalyzer.analyze(text, sender, recentMessagesFromSender)
        urgencyScore += contextResult.signalScore
        contributingFactors.addAll(contextResult.factors)

        // Normalize urgency score to 0-1 range
        val normalizedUrgency = urgencyScore.coerceIn(0f, 1f)

        // Determine final priority
        val finalPriority = determineFinalPriority(
            currentPriority = currentPriority,
            deadlineResult = deadlineResult,
            normalizedUrgency = normalizedUrgency
        )

        val shouldEscalate = finalPriority.ordinal > currentPriority.ordinal ||
            normalizedUrgency >= ESCALATION_THRESHOLD

        return PrioritizationResult(
            priority = finalPriority,
            urgencyScore = normalizedUrgency,
            contributingFactors = contributingFactors,
            shouldEscalate = shouldEscalate
        )
    }

    private fun determineFinalPriority(
        currentPriority: TaskPriority,
        deadlineResult: DeadlineEscalationResult?,
        normalizedUrgency: Float
    ): TaskPriority {
        // Deadline escalation takes highest precedence
        if (deadlineResult != null && deadlineResult.escalatedPriority.ordinal > currentPriority.ordinal) {
            return deadlineResult.escalatedPriority
        }

        // Urgency score determines escalation
        return when {
            normalizedUrgency >= 0.85f -> TaskPriority.CRITICAL
            normalizedUrgency >= 0.65f -> maxPriority(currentPriority, TaskPriority.HIGH)
            normalizedUrgency >= 0.45f -> maxPriority(currentPriority, TaskPriority.MEDIUM)
            else -> currentPriority
        }
    }

    private fun priorityToBaseScore(priority: TaskPriority): Float {
        return when (priority) {
            TaskPriority.LOW -> 0.1f
            TaskPriority.MEDIUM -> 0.3f
            TaskPriority.HIGH -> 0.5f
            TaskPriority.CRITICAL -> 0.7f
        }
    }

    private fun maxPriority(a: TaskPriority, b: TaskPriority): TaskPriority {
        return if (a.ordinal >= b.ordinal) a else b
    }
}
