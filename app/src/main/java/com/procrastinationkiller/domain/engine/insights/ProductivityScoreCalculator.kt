package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductivityScoreCalculator @Inject constructor() {

    companion object {
        private const val COMPLETION_WEIGHT = 0.35f
        private const val VELOCITY_WEIGHT = 0.25f
        private const val RESPONSE_WEIGHT = 0.20f
        private const val BACKLOG_WEIGHT = 0.20f
        private const val MAX_SCORE = 100
    }

    fun calculateScore(
        tasks: List<TaskEntity>,
        velocityTrend: VelocityTrend,
        senderPatterns: List<SenderResponsePattern>
    ): ProductivityScore {
        val completionRate = computeCompletionRate(tasks)
        val velocityScore = computeVelocityScore(velocityTrend)
        val responseScore = computeResponseScore(senderPatterns)
        val backlogScore = computeBacklogScore(tasks)

        val overall = (
            completionRate * COMPLETION_WEIGHT +
            velocityScore * VELOCITY_WEIGHT +
            responseScore * RESPONSE_WEIGHT +
            backlogScore * BACKLOG_WEIGHT
        ).toInt().coerceIn(0, MAX_SCORE)

        return ProductivityScore(
            overall = overall,
            completionRate = completionRate / 100f,
            velocityScore = velocityScore / 100f,
            responseScore = responseScore / 100f,
            backlogScore = backlogScore / 100f
        )
    }

    fun computeCompletionRate(tasks: List<TaskEntity>): Float {
        if (tasks.isEmpty()) return 0f

        val completed = tasks.count { it.status == "COMPLETED" }
        return (completed.toFloat() / tasks.size * 100f).coerceIn(0f, 100f)
    }

    fun computeVelocityScore(velocityTrend: VelocityTrend): Float {
        return when (velocityTrend.trend) {
            TrendDirection.IMPROVING -> {
                val bonus = (velocityTrend.slope * 20).coerceIn(0.0, 30.0).toFloat()
                (70f + bonus).coerceIn(0f, 100f)
            }
            TrendDirection.STABLE -> 50f
            TrendDirection.DECLINING -> {
                val penalty = ((-velocityTrend.slope) * 20).coerceIn(0.0, 30.0).toFloat()
                (30f - penalty).coerceIn(0f, 100f)
            }
        }
    }

    fun computeResponseScore(senderPatterns: List<SenderResponsePattern>): Float {
        if (senderPatterns.isEmpty()) return 50f

        val avgResponseMs = senderPatterns.map { it.avgResponseTimeMs }.average()
        val oneDayMs = 24.0 * 60 * 60 * 1000

        return when {
            avgResponseMs < oneDayMs -> 100f
            avgResponseMs < 2 * oneDayMs -> 80f
            avgResponseMs < 3 * oneDayMs -> 60f
            avgResponseMs < 5 * oneDayMs -> 40f
            avgResponseMs < 7 * oneDayMs -> 20f
            else -> 10f
        }
    }

    fun computeBacklogScore(tasks: List<TaskEntity>): Float {
        val pending = tasks.count { it.status == "PENDING" || it.status == "IN_PROGRESS" }
        val total = tasks.size

        if (total == 0) return 100f

        val backlogRatio = pending.toFloat() / total
        return ((1f - backlogRatio) * 100f).coerceIn(0f, 100f)
    }
}
