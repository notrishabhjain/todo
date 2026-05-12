package com.procrastinationkiller.util

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.repository.TaskRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoDataGenerator @Inject constructor(
    private val taskRepository: TaskRepository
) {

    suspend fun generateDemoData() {
        val tasks = createSampleTasks()
        for (task in tasks) {
            taskRepository.insertTask(task)
        }
    }

    fun createSampleTasks(): List<TaskEntity> {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        return listOf(
            TaskEntity(
                title = "Review PR for authentication module",
                description = "Check the code changes in the auth module PR",
                priority = "HIGH",
                status = "PENDING",
                reminderMode = "NORMAL",
                deadline = now + dayMs,
                createdAt = now - 2 * dayMs
            ),
            TaskEntity(
                title = "Submit expense report",
                description = "Submit last month's expense report to finance",
                priority = "MEDIUM",
                status = "PENDING",
                reminderMode = "NORMAL",
                deadline = now + 3 * dayMs,
                createdAt = now - dayMs
            ),
            TaskEntity(
                title = "Call dentist for appointment",
                description = "Schedule dental checkup appointment",
                priority = "LOW",
                status = "PENDING",
                reminderMode = "GENTLE",
                deadline = now + 7 * dayMs,
                createdAt = now - 5 * dayMs
            ),
            TaskEntity(
                title = "Deploy hotfix to production",
                description = "Critical bug fix needs to be deployed ASAP",
                priority = "CRITICAL",
                status = "IN_PROGRESS",
                reminderMode = "AGGRESSIVE",
                deadline = now + dayMs / 2,
                createdAt = now - dayMs / 4
            ),
            TaskEntity(
                title = "Send meeting notes to team",
                description = "Share the notes from today's standup",
                priority = "MEDIUM",
                status = "COMPLETED",
                reminderMode = "NORMAL",
                deadline = now,
                createdAt = now - dayMs,
                completedAt = now - dayMs / 2
            ),
            TaskEntity(
                title = "Prepare quarterly presentation",
                description = "Create slides for Q4 review meeting",
                priority = "HIGH",
                status = "COMPLETED",
                reminderMode = "NORMAL",
                deadline = now - dayMs,
                createdAt = now - 5 * dayMs,
                completedAt = now - dayMs / 4
            ),
            TaskEntity(
                title = "Update project dependencies",
                description = "Upgrade all outdated npm packages",
                priority = "LOW",
                status = "PENDING",
                reminderMode = "GENTLE",
                createdAt = now - 10 * dayMs
            ),
            TaskEntity(
                title = "Reply to client email",
                description = "Respond to the client's questions about timeline",
                priority = "HIGH",
                status = "COMPLETED",
                reminderMode = "NORMAL",
                deadline = now - 2 * dayMs,
                createdAt = now - 3 * dayMs,
                completedAt = now - 2 * dayMs
            ),
            TaskEntity(
                title = "Fix login page CSS issue",
                description = "Button alignment is broken on mobile viewport",
                priority = "MEDIUM",
                status = "PENDING",
                reminderMode = "NORMAL",
                deadline = now + 2 * dayMs,
                createdAt = now - dayMs
            ),
            TaskEntity(
                title = "Order team lunch for Friday",
                description = "Book catering for team celebration",
                priority = "MEDIUM",
                status = "PENDING",
                reminderMode = "NORMAL",
                deadline = now + 4 * dayMs,
                createdAt = now
            )
        )
    }
}
