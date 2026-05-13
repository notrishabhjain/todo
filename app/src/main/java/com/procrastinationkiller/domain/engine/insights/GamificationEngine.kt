package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationEngine @Inject constructor() {

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000

        val ACHIEVEMENT_DEFINITIONS = listOf(
            AchievementDefinition("first_task", "First Step", "Complete your first task", 1),
            AchievementDefinition("ten_tasks", "Getting Things Done", "Complete 10 tasks", 10),
            AchievementDefinition("fifty_tasks", "Productivity Pro", "Complete 50 tasks", 50),
            AchievementDefinition("hundred_tasks", "Centurion", "Complete 100 tasks", 100),
            AchievementDefinition("three_day_streak", "On a Roll", "Maintain a 3-day streak", 0),
            AchievementDefinition("seven_day_streak", "Week Warrior", "Maintain a 7-day streak", 0),
            AchievementDefinition("thirty_day_streak", "Monthly Master", "Maintain a 30-day streak", 0),
            AchievementDefinition("perfect_week", "Perfect Week", "Complete at least one task every day for a week", 0),
            AchievementDefinition("early_bird", "Early Bird", "Complete 5 tasks before 9 AM", 5),
            AchievementDefinition("night_owl", "Night Owl", "Complete 5 tasks after 9 PM", 5)
        )
    }

    data class AchievementDefinition(
        val type: String,
        val title: String,
        val description: String,
        val threshold: Int
    )

    fun computeStreak(tasks: List<TaskEntity>): StreakInfo {
        val completedTasks = tasks.filter { it.status == "COMPLETED" && it.completedAt != null }

        if (completedTasks.isEmpty()) {
            return StreakInfo()
        }

        val completionDays = completedTasks
            .map { getStartOfDay(it.completedAt!!) }
            .distinct()
            .sorted()

        if (completionDays.isEmpty()) {
            return StreakInfo()
        }

        val todayStart = getStartOfDay(System.currentTimeMillis())
        val yesterdayStart = todayStart - DAY_MS

        // Calculate current streak (consecutive days ending today or yesterday)
        var currentStreak = 0
        var checkDay = if (completionDays.contains(todayStart)) todayStart else yesterdayStart

        if (!completionDays.contains(checkDay)) {
            currentStreak = 0
        } else {
            while (completionDays.contains(checkDay)) {
                currentStreak++
                checkDay -= DAY_MS
            }
        }

        // Calculate longest streak
        var longestStreak = 0
        var tempStreak = 1
        for (i in 1 until completionDays.size) {
            if (completionDays[i] - completionDays[i - 1] == DAY_MS) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastActiveDay = completionDays.last()
        )
    }

    fun computeAchievements(tasks: List<TaskEntity>, streak: StreakInfo): List<Achievement> {
        val completedTasks = tasks.filter { it.status == "COMPLETED" && it.completedAt != null }
        val totalCompleted = completedTasks.size

        return ACHIEVEMENT_DEFINITIONS.map { definition ->
            val isUnlocked = when (definition.type) {
                "first_task" -> totalCompleted >= 1
                "ten_tasks" -> totalCompleted >= 10
                "fifty_tasks" -> totalCompleted >= 50
                "hundred_tasks" -> totalCompleted >= 100
                "three_day_streak" -> streak.longestStreak >= 3
                "seven_day_streak" -> streak.longestStreak >= 7
                "thirty_day_streak" -> streak.longestStreak >= 30
                "perfect_week" -> streak.longestStreak >= 7
                "early_bird" -> countEarlyBirdTasks(completedTasks) >= 5
                "night_owl" -> countNightOwlTasks(completedTasks) >= 5
                else -> false
            }

            Achievement(
                type = definition.type,
                title = definition.title,
                description = definition.description,
                isUnlocked = isUnlocked,
                unlockedAt = if (isUnlocked) completedTasks.lastOrNull()?.completedAt else null
            )
        }
    }

    fun getNextGoal(achievements: List<Achievement>): Achievement? {
        return achievements.firstOrNull { !it.isUnlocked }
    }

    fun computeGamificationState(tasks: List<TaskEntity>): GamificationState {
        val streak = computeStreak(tasks)
        val achievements = computeAchievements(tasks, streak)
        val nextGoal = getNextGoal(achievements)
        val totalCompleted = tasks.count { it.status == "COMPLETED" }

        return GamificationState(
            streak = streak,
            achievements = achievements,
            nextGoal = nextGoal,
            totalTasksCompleted = totalCompleted
        )
    }

    private fun countEarlyBirdTasks(tasks: List<TaskEntity>): Int {
        return tasks.count { task ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = task.completedAt!!
            calendar.get(Calendar.HOUR_OF_DAY) < 9
        }
    }

    private fun countNightOwlTasks(tasks: List<TaskEntity>): Int {
        return tasks.count { task ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = task.completedAt!!
            calendar.get(Calendar.HOUR_OF_DAY) >= 21
        }
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
}
