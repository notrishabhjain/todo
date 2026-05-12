package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VelocityTracker @Inject constructor() {

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val TREND_THRESHOLD = 0.1
    }

    fun computeVelocityTrend(tasks: List<TaskEntity>, daysBack: Int = 14): VelocityTrend {
        val completedTasks = tasks.filter { it.status == "COMPLETED" && it.completedAt != null }

        if (completedTasks.isEmpty()) {
            return VelocityTrend()
        }

        val now = System.currentTimeMillis()
        val startTime = now - (daysBack * DAY_MS)

        val dailyCompletions = (0 until daysBack).map { dayOffset ->
            val dayStart = startTime + (dayOffset * DAY_MS)
            val dayEnd = dayStart + DAY_MS

            val count = completedTasks.count { task ->
                task.completedAt!! in dayStart until dayEnd
            }

            DailyCompletion(
                dayTimestamp = dayStart,
                count = count
            )
        }

        val slope = linearRegressionSlope(dailyCompletions.map { it.count.toDouble() })

        val trend = when {
            slope > TREND_THRESHOLD -> TrendDirection.IMPROVING
            slope < -TREND_THRESHOLD -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }

        return VelocityTrend(
            dailyCompletions = dailyCompletions,
            slope = slope,
            trend = trend
        )
    }

    fun linearRegressionSlope(values: List<Double>): Double {
        if (values.size < 2) return 0.0

        val n = values.size.toDouble()
        val indices = values.indices.map { it.toDouble() }

        val sumX = indices.sum()
        val sumY = values.sum()
        val sumXY = indices.zip(values) { x, y -> x * y }.sum()
        val sumX2 = indices.sumOf { it * it }

        val denominator = n * sumX2 - sumX * sumX
        if (denominator == 0.0) return 0.0

        return (n * sumXY - sumX * sumY) / denominator
    }
}
