package com.procrastinationkiller.data.export

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.usecase.FakeTaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExportImportServiceTest {

    private lateinit var service: ExportImportService
    private lateinit var fakeRepository: FakeTaskRepository

    @BeforeEach
    fun setup() {
        fakeRepository = FakeTaskRepository()
        service = ExportImportService(
            taskRepository = fakeRepository,
            csvExporter = CsvExporter(),
            jsonExporter = JsonExporter(),
            csvImporter = CsvImporter(),
            jsonImporter = JsonImporter()
        )
    }

    @Test
    fun `export CSV returns correct task count`() = runBlocking {
        fakeRepository.setTasks(createSampleTasks())

        val result = service.exportTasks(ExportFormat.CSV)

        assertEquals(2, result.taskCount)
        assertEquals(ExportFormat.CSV, result.format)
        assertTrue(result.content.isNotEmpty())
    }

    @Test
    fun `export JSON returns correct task count`() = runBlocking {
        fakeRepository.setTasks(createSampleTasks())

        val result = service.exportTasks(ExportFormat.JSON)

        assertEquals(2, result.taskCount)
        assertEquals(ExportFormat.JSON, result.format)
        assertTrue(result.content.isNotEmpty())
    }

    @Test
    fun `import CSV creates tasks in repository`() = runBlocking {
        val csv = "title,description,priority,status,reminder_mode,deadline,created_at,completed_at\n" +
            "Task 1,Desc 1,HIGH,PENDING,NORMAL,,1000,\n" +
            "Task 2,Desc 2,LOW,COMPLETED,GENTLE,,2000,3000"

        val result = service.importTasks(csv, ExportFormat.CSV)

        assertEquals(2, result.importedCount)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `import JSON creates tasks in repository`() = runBlocking {
        val json = """[
            {"title":"Task 1","description":"Desc","priority":"HIGH","status":"PENDING","reminderMode":"NORMAL","createdAt":1000},
            {"title":"Task 2","description":"Desc 2","priority":"LOW","status":"COMPLETED","reminderMode":"GENTLE","createdAt":2000,"completedAt":3000}
        ]"""

        val result = service.importTasks(json, ExportFormat.JSON)

        assertEquals(2, result.importedCount)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `import with errors reports partial success`() = runBlocking {
        val csv = "title,description,priority\nValid Task,Desc,HIGH\nX,Y"

        val result = service.importTasks(csv, ExportFormat.CSV)

        assertEquals(1, result.importedCount)
        assertEquals(1, result.errors.size)
    }

    private fun createSampleTasks(): List<TaskEntity> = listOf(
        TaskEntity(id = 1, title = "Task A", priority = "HIGH", status = "PENDING", createdAt = 1000L),
        TaskEntity(id = 2, title = "Task B", priority = "LOW", status = "COMPLETED", createdAt = 2000L, completedAt = 3000L)
    )
}
