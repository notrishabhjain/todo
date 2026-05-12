package com.procrastinationkiller.util

import com.procrastinationkiller.domain.usecase.FakeTaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DemoDataGeneratorTest {

    private lateinit var demoDataGenerator: DemoDataGenerator
    private lateinit var fakeRepository: FakeTaskRepository

    @BeforeEach
    fun setup() {
        fakeRepository = FakeTaskRepository()
        demoDataGenerator = DemoDataGenerator(fakeRepository)
    }

    @Test
    fun `creates sample tasks returns non-empty list`() {
        val tasks = demoDataGenerator.createSampleTasks()
        assertTrue(tasks.isNotEmpty())
        assertTrue(tasks.size >= 5)
    }

    @Test
    fun `sample tasks have varied priorities`() {
        val tasks = demoDataGenerator.createSampleTasks()
        val priorities = tasks.map { it.priority }.toSet()
        assertTrue(priorities.size >= 3)
    }

    @Test
    fun `sample tasks have varied statuses`() {
        val tasks = demoDataGenerator.createSampleTasks()
        val statuses = tasks.map { it.status }.toSet()
        assertTrue(statuses.size >= 2)
    }

    @Test
    fun `generate demo data inserts into repository`() = runBlocking {
        demoDataGenerator.generateDemoData()

        val allTasks = fakeRepository.getAllTasks().first()
        assertTrue(allTasks.isNotEmpty())
        assertEquals(demoDataGenerator.createSampleTasks().size, allTasks.size)
    }

    @Test
    fun `sample tasks have titles and valid timestamps`() {
        val tasks = demoDataGenerator.createSampleTasks()
        for (task in tasks) {
            assertTrue(task.title.isNotEmpty())
            assertTrue(task.createdAt > 0)
        }
    }

    @Test
    fun `completed tasks have completedAt set`() {
        val tasks = demoDataGenerator.createSampleTasks()
        val completed = tasks.filter { it.status == "COMPLETED" }
        for (task in completed) {
            assertTrue(task.completedAt != null && task.completedAt!! > 0)
        }
    }
}
