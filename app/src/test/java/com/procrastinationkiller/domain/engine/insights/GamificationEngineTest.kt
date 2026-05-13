package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Calendar

class GamificationEngineTest {

    private lateinit var engine: GamificationEngine

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }

    @BeforeEach
    fun setup() {
        engine = GamificationEngine()
    }

    @Test
    fun `computeStreak returns zero for no tasks`() {
        val result = engine.computeStreak(emptyList())
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun `computeStreak counts consecutive days correctly`() {
        val now = System.currentTimeMillis()
        val todayStart = getStartOfDay(now)

        val tasks = listOf(
            createTask(1, completedAt = todayStart + 3600000), // today
            createTask(2, completedAt = todayStart - DAY_MS + 3600000), // yesterday
            createTask(3, completedAt = todayStart - 2 * DAY_MS + 3600000) // 2 days ago
        )

        val result = engine.computeStreak(tasks)
        assertEquals(3, result.currentStreak)
    }

    @Test
    fun `computeStreak resets on missed day`() {
        val now = System.currentTimeMillis()
        val todayStart = getStartOfDay(now)

        val tasks = listOf(
            createTask(1, completedAt = todayStart + 3600000), // today
            // yesterday - no task (gap)
            createTask(2, completedAt = todayStart - 2 * DAY_MS + 3600000) // 2 days ago
        )

        val result = engine.computeStreak(tasks)
        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `computeStreak calculates longest streak`() {
        val now = System.currentTimeMillis()
        val todayStart = getStartOfDay(now)

        // 5-day streak in the past, then a gap, then 2-day current streak
        val tasks = listOf(
            createTask(1, completedAt = todayStart + 3600000), // today
            createTask(2, completedAt = todayStart - DAY_MS + 3600000), // yesterday
            // gap
            createTask(3, completedAt = todayStart - 10 * DAY_MS + 3600000),
            createTask(4, completedAt = todayStart - 11 * DAY_MS + 3600000),
            createTask(5, completedAt = todayStart - 12 * DAY_MS + 3600000),
            createTask(6, completedAt = todayStart - 13 * DAY_MS + 3600000),
            createTask(7, completedAt = todayStart - 14 * DAY_MS + 3600000)
        )

        val result = engine.computeStreak(tasks)
        assertEquals(2, result.currentStreak)
        assertEquals(5, result.longestStreak)
    }

    @Test
    fun `computeAchievements unlocks first_task with 1 completed`() {
        val tasks = listOf(
            createTask(1, completedAt = System.currentTimeMillis())
        )
        val streak = StreakInfo(currentStreak = 1, longestStreak = 1)

        val achievements = engine.computeAchievements(tasks, streak)
        val firstTask = achievements.find { it.type == "first_task" }

        assertNotNull(firstTask)
        assertTrue(firstTask!!.isUnlocked)
    }

    @Test
    fun `computeAchievements does not unlock ten_tasks with fewer than 10`() {
        val tasks = (1..5L).map { createTask(it, completedAt = System.currentTimeMillis()) }
        val streak = StreakInfo(currentStreak = 1, longestStreak = 1)

        val achievements = engine.computeAchievements(tasks, streak)
        val tenTasks = achievements.find { it.type == "ten_tasks" }

        assertNotNull(tenTasks)
        assertFalse(tenTasks!!.isUnlocked)
    }

    @Test
    fun `computeAchievements unlocks seven_day_streak`() {
        val tasks = (1..10L).map { createTask(it, completedAt = System.currentTimeMillis()) }
        val streak = StreakInfo(currentStreak = 7, longestStreak = 7)

        val achievements = engine.computeAchievements(tasks, streak)
        val sevenDay = achievements.find { it.type == "seven_day_streak" }

        assertNotNull(sevenDay)
        assertTrue(sevenDay!!.isUnlocked)
    }

    @Test
    fun `getNextGoal returns first locked achievement`() {
        val achievements = listOf(
            Achievement("first_task", "First Step", "Complete first task", isUnlocked = true),
            Achievement("ten_tasks", "Getting Things Done", "Complete 10 tasks", isUnlocked = false),
            Achievement("fifty_tasks", "Productivity Pro", "Complete 50 tasks", isUnlocked = false)
        )

        val next = engine.getNextGoal(achievements)
        assertNotNull(next)
        assertEquals("ten_tasks", next!!.type)
    }

    @Test
    fun `computeGamificationState combines streak and achievements`() {
        val now = System.currentTimeMillis()
        val todayStart = getStartOfDay(now)

        val tasks = (1..10L).map { id ->
            createTask(id, completedAt = todayStart + 3600000)
        }

        val state = engine.computeGamificationState(tasks)
        assertEquals(10, state.totalTasksCompleted)
        assertTrue(state.achievements.any { it.type == "first_task" && it.isUnlocked })
        assertTrue(state.achievements.any { it.type == "ten_tasks" && it.isUnlocked })
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

    private fun createTask(id: Long, completedAt: Long? = null): TaskEntity {
        return TaskEntity(
            id = id,
            title = "Task $id",
            description = "Description",
            priority = "MEDIUM",
            status = if (completedAt != null) "COMPLETED" else "PENDING",
            createdAt = System.currentTimeMillis() - DAY_MS * 30,
            completedAt = completedAt
        )
    }
}
