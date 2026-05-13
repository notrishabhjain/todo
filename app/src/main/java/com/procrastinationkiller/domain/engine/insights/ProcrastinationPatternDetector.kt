package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcrastinationPatternDetector @Inject constructor() {

    companion object {
        private const val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000
    }

    fun detectPatterns(tasks: List<TaskEntity>): List<ProcrastinationPattern> {
        val completedTasks = tasks.filter { it.status == "COMPLETED" && it.completedAt != null }

        if (completedTasks.isEmpty()) {
            return emptyList()
        }

        val delayedTasks = completedTasks.filter { task ->
            val delay = task.completedAt!! - task.createdAt
            delay > THREE_DAYS_MS
        }

        if (delayedTasks.isEmpty()) {
            return emptyList()
        }

        val byPriority = delayedTasks.groupBy { it.priority }

        return byPriority.map { (priority, tasks) ->
            val avgDelay = tasks.map { it.completedAt!! - it.createdAt }.average().toLong()
            ProcrastinationPattern(
                category = priority,
                avgDelayMs = avgDelay,
                count = tasks.size
            )
        }.sortedByDescending { it.avgDelayMs }
    }

    fun detectPendingPatterns(tasks: List<TaskEntity>): List<ProcrastinationPattern> {
        val now = System.currentTimeMillis()
        val pendingTasks = tasks.filter {
            it.status == "PENDING" || it.status == "IN_PROGRESS"
        }

        if (pendingTasks.isEmpty()) {
            return emptyList()
        }

        val staleTasks = pendingTasks.filter { task ->
            (now - task.createdAt) > THREE_DAYS_MS
        }

        if (staleTasks.isEmpty()) {
            return emptyList()
        }

        val byPriority = staleTasks.groupBy { it.priority }

        return byPriority.map { (priority, tasks) ->
            val avgDelay = tasks.map { now - it.createdAt }.average().toLong()
            ProcrastinationPattern(
                category = "PENDING_$priority",
                avgDelayMs = avgDelay,
                count = tasks.size
            )
        }.sortedByDescending { it.avgDelayMs }
    }
}
