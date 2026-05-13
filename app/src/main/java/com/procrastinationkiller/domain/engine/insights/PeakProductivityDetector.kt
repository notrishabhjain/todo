package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeakProductivityDetector @Inject constructor() {

    fun detectPeakHours(tasks: List<TaskEntity>): List<HourlyProductivity> {
        val completedTasks = tasks.filter { it.status == "COMPLETED" && it.completedAt != null }

        if (completedTasks.isEmpty()) {
            return emptyList()
        }

        val hourlyGroups = completedTasks.groupBy { task ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = task.completedAt!!
            calendar.get(Calendar.HOUR_OF_DAY)
        }

        return (0..23).map { hour ->
            val tasksInHour = hourlyGroups[hour] ?: emptyList()
            val avgSpeed = if (tasksInHour.isNotEmpty()) {
                tasksInHour.mapNotNull { task ->
                    task.completedAt?.let { it - task.createdAt }
                }.let { durations ->
                    if (durations.isNotEmpty()) durations.average().toLong() else 0L
                }
            } else {
                0L
            }

            HourlyProductivity(
                hour = hour,
                completionCount = tasksInHour.size,
                avgCompletionSpeedMs = avgSpeed
            )
        }.filter { it.completionCount > 0 }
            .sortedByDescending { it.completionCount }
    }

    fun getTopPeakHours(tasks: List<TaskEntity>, count: Int = 3): List<HourlyProductivity> {
        return detectPeakHours(tasks).take(count)
    }
}
