package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BottleneckDetector @Inject constructor() {

    companion object {
        private const val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }

    fun detectBottlenecks(tasks: List<TaskEntity>): List<Bottleneck> {
        val bottlenecks = mutableListOf<Bottleneck>()

        detectStaleTasks(tasks, bottlenecks)
        detectOverdueTasks(tasks, bottlenecks)
        detectInactivePeriods(tasks, bottlenecks)

        return bottlenecks.sortedByDescending { it.severity }
    }

    private fun detectStaleTasks(tasks: List<TaskEntity>, bottlenecks: MutableList<Bottleneck>) {
        val now = System.currentTimeMillis()
        val staleTasks = tasks.filter { task ->
            (task.status == "PENDING" || task.status == "IN_PROGRESS") &&
                (now - task.createdAt) > THREE_DAYS_MS
        }

        if (staleTasks.isNotEmpty()) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.STALE_TASKS,
                    description = "You have ${staleTasks.size} tasks stuck for more than 3 days. Consider prioritizing or removing them.",
                    affectedCount = staleTasks.size,
                    severity = (staleTasks.size * 2).coerceAtMost(10)
                )
            )
        }
    }

    private fun detectOverdueTasks(tasks: List<TaskEntity>, bottlenecks: MutableList<Bottleneck>) {
        val now = System.currentTimeMillis()
        val overdueTasks = tasks.filter { task ->
            task.status != "COMPLETED" &&
                task.deadline != null &&
                task.deadline < now
        }

        if (overdueTasks.isNotEmpty()) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.OVERDUE_TASKS,
                    description = "${overdueTasks.size} tasks are past their deadline. Address or reschedule them.",
                    affectedCount = overdueTasks.size,
                    severity = (overdueTasks.size * 3).coerceAtMost(10)
                )
            )
        }
    }

    private fun detectInactivePeriods(tasks: List<TaskEntity>, bottlenecks: MutableList<Bottleneck>) {
        val completedTasks = tasks.filter { it.status == "COMPLETED" && it.completedAt != null }
            .sortedBy { it.completedAt }

        if (completedTasks.size < 2) return

        val now = System.currentTimeMillis()
        val lastCompletion = completedTasks.last().completedAt!!

        if (now - lastCompletion > SEVEN_DAYS_MS) {
            val daysSinceActivity = (now - lastCompletion) / (24 * 60 * 60 * 1000)
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.INACTIVE_PERIOD,
                    description = "No tasks completed in $daysSinceActivity days. Getting back on track today will help.",
                    affectedCount = 0,
                    severity = 8
                )
            )
        }
    }
}
