package com.procrastinationkiller.domain.engine.prioritization

import com.procrastinationkiller.domain.model.TaskPriority
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeadlineProximityEscalator @Inject constructor() {

    companion object {
        private const val ONE_HOUR_MS = 60L * 60 * 1000
        private const val FOUR_HOURS_MS = 4L * 60 * 60 * 1000
        private const val TWENTY_FOUR_HOURS_MS = 24L * 60 * 60 * 1000
    }

    fun evaluate(
        deadlineMs: Long?,
        currentPriority: TaskPriority,
        currentTimeMs: Long = System.currentTimeMillis()
    ): DeadlineEscalationResult? {
        if (deadlineMs == null) return null

        val timeRemainingMs = deadlineMs - currentTimeMs
        if (timeRemainingMs <= 0) {
            // Already past deadline
            return DeadlineEscalationResult(
                escalatedPriority = TaskPriority.CRITICAL,
                urgencyMultiplier = 1.0f,
                reason = "DEADLINE_PASSED"
            )
        }

        return when {
            timeRemainingMs <= ONE_HOUR_MS -> DeadlineEscalationResult(
                escalatedPriority = TaskPriority.CRITICAL,
                urgencyMultiplier = 0.9f,
                reason = "DEADLINE_WITHIN_1_HOUR"
            )
            timeRemainingMs <= FOUR_HOURS_MS -> DeadlineEscalationResult(
                escalatedPriority = TaskPriority.HIGH,
                urgencyMultiplier = 0.7f,
                reason = "DEADLINE_WITHIN_4_HOURS"
            )
            timeRemainingMs <= TWENTY_FOUR_HOURS_MS -> {
                val escalated = escalateOneLevel(currentPriority)
                DeadlineEscalationResult(
                    escalatedPriority = escalated,
                    urgencyMultiplier = 0.5f,
                    reason = "DEADLINE_WITHIN_24_HOURS"
                )
            }
            else -> null
        }
    }

    private fun escalateOneLevel(current: TaskPriority): TaskPriority {
        return when (current) {
            TaskPriority.LOW -> TaskPriority.MEDIUM
            TaskPriority.MEDIUM -> TaskPriority.HIGH
            TaskPriority.HIGH -> TaskPriority.CRITICAL
            TaskPriority.CRITICAL -> TaskPriority.CRITICAL
        }
    }
}

data class DeadlineEscalationResult(
    val escalatedPriority: TaskPriority,
    val urgencyMultiplier: Float,
    val reason: String
)
