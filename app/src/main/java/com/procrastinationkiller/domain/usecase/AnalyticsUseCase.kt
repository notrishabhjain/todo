package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class AnalyticsData(
    val completedToday: Int = 0,
    val completedThisWeek: Int = 0,
    val completedThisMonth: Int = 0,
    val pendingBacklog: Int = 0,
    val completionRate: Float = 0f,
    val mostIgnoredTasks: List<TaskEntity> = emptyList(),
    val productivityTrend: List<DailyProductivity> = emptyList()
)

data class DailyProductivity(
    val date: Long,
    val completedCount: Int,
    val createdCount: Int
)

enum class TimeRange {
    HOUR,
    DAY,
    WEEK,
    MONTH,
    CUSTOM
}

@Singleton
class AnalyticsUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    fun getAnalytics(timeRange: TimeRange = TimeRange.WEEK): Flow<AnalyticsData> {
        return taskRepository.getAllTasks().map { tasks ->
            computeAnalytics(tasks, timeRange)
        }
    }

    fun computeAnalytics(tasks: List<TaskEntity>, timeRange: TimeRange): AnalyticsData {
        val now = System.currentTimeMillis()
        val todayStart = getStartOfDay(now)
        val weekStart = getStartOfWeek(now)
        val monthStart = getStartOfMonth(now)

        val completedTasks = tasks.filter { it.status == "COMPLETED" }
        val pendingTasks = tasks.filter { it.status == "PENDING" || it.status == "IN_PROGRESS" }

        val completedToday = completedTasks.count { (it.completedAt ?: 0) >= todayStart }
        val completedThisWeek = completedTasks.count { (it.completedAt ?: 0) >= weekStart }
        val completedThisMonth = completedTasks.count { (it.completedAt ?: 0) >= monthStart }

        val totalTasks = tasks.size
        val completionRate = if (totalTasks > 0) {
            completedTasks.size.toFloat() / totalTasks.toFloat()
        } else {
            0f
        }

        // Most ignored: pending tasks that are oldest (created longest ago without completion)
        val mostIgnored = pendingTasks
            .sortedBy { it.createdAt }
            .take(5)

        val productivityTrend = computeProductivityTrend(tasks, timeRange, now)

        return AnalyticsData(
            completedToday = completedToday,
            completedThisWeek = completedThisWeek,
            completedThisMonth = completedThisMonth,
            pendingBacklog = pendingTasks.size,
            completionRate = completionRate,
            mostIgnoredTasks = mostIgnored,
            productivityTrend = productivityTrend
        )
    }

    private fun computeProductivityTrend(
        tasks: List<TaskEntity>,
        timeRange: TimeRange,
        now: Long
    ): List<DailyProductivity> {
        val daysBack = when (timeRange) {
            TimeRange.HOUR -> 1
            TimeRange.DAY -> 1
            TimeRange.WEEK -> 7
            TimeRange.MONTH -> 30
            TimeRange.CUSTOM -> 14
        }

        val result = mutableListOf<DailyProductivity>()
        val calendar = Calendar.getInstance()

        for (i in daysBack - 1 downTo 0) {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = getStartOfDay(calendar.timeInMillis)
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L

            val completedCount = tasks.count { task ->
                task.status == "COMPLETED" &&
                    (task.completedAt ?: 0) >= dayStart &&
                    (task.completedAt ?: 0) < dayEnd
            }
            val createdCount = tasks.count { task ->
                task.createdAt >= dayStart && task.createdAt < dayEnd
            }

            result.add(DailyProductivity(dayStart, completedCount, createdCount))
        }

        return result
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfWeek(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
