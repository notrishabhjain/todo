package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Calendar

class PeakProductivityDetectorTest {

    private lateinit var detector: PeakProductivityDetector

    @BeforeEach
    fun setup() {
        detector = PeakProductivityDetector()
    }

    @Test
    fun `detectPeakHours returns empty for no tasks`() {
        val result = detector.detectPeakHours(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectPeakHours returns empty for tasks without completedAt`() {
        val tasks = listOf(
            createTask(id = 1, status = "PENDING", completedAt = null)
        )
        val result = detector.detectPeakHours(tasks)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectPeakHours identifies correct peak hour`() {
        val hour10 = createTimestampAtHour(10)
        val hour10b = createTimestampAtHour(10) + 60000
        val hour14 = createTimestampAtHour(14)

        val tasks = listOf(
            createTask(id = 1, status = "COMPLETED", completedAt = hour10),
            createTask(id = 2, status = "COMPLETED", completedAt = hour10b),
            createTask(id = 3, status = "COMPLETED", completedAt = hour14)
        )

        val result = detector.detectPeakHours(tasks)

        assertTrue(result.isNotEmpty())
        assertEquals(10, result.first().hour)
        assertEquals(2, result.first().completionCount)
    }

    @Test
    fun `getTopPeakHours returns at most count results`() {
        val tasks = (0..23).map { hour ->
            createTask(
                id = hour.toLong(),
                status = "COMPLETED",
                completedAt = createTimestampAtHour(hour)
            )
        }

        val result = detector.getTopPeakHours(tasks, count = 3)
        assertTrue(result.size <= 3)
    }

    @Test
    fun `detectPeakHours only considers completed tasks`() {
        val hour10 = createTimestampAtHour(10)

        val tasks = listOf(
            createTask(id = 1, status = "COMPLETED", completedAt = hour10),
            createTask(id = 2, status = "PENDING", completedAt = null),
            createTask(id = 3, status = "IN_PROGRESS", completedAt = null)
        )

        val result = detector.detectPeakHours(tasks)
        assertEquals(1, result.size)
        assertEquals(1, result.first().completionCount)
    }

    private fun createTimestampAtHour(hour: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun createTask(
        id: Long = 0,
        status: String = "PENDING",
        completedAt: Long? = null
    ): TaskEntity {
        return TaskEntity(
            id = id,
            title = "Task $id",
            description = "Description $id",
            priority = "MEDIUM",
            status = status,
            createdAt = System.currentTimeMillis() - 86400000,
            completedAt = completedAt
        )
    }
}
