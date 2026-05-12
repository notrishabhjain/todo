package com.procrastinationkiller.data.export

import com.procrastinationkiller.data.local.entity.TaskEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExportImportRoundTripTest {

    private lateinit var csvExporter: CsvExporter
    private lateinit var csvImporter: CsvImporter
    private lateinit var jsonExporter: JsonExporter
    private lateinit var jsonImporter: JsonImporter

    @BeforeEach
    fun setup() {
        csvExporter = CsvExporter()
        csvImporter = CsvImporter()
        jsonExporter = JsonExporter()
        jsonImporter = JsonImporter()
    }

    @Test
    fun `CSV export and import round trip preserves data`() {
        val tasks = createSampleTasks()

        val csv = csvExporter.export(tasks)
        val imported = csvImporter.import(csv)

        assertEquals(tasks.size, imported.size)
        for ((index, result) in imported.withIndex()) {
            assertTrue(result.isSuccess)
            val importedTask = result.getOrThrow()
            assertEquals(tasks[index].title, importedTask.title)
            assertEquals(tasks[index].description, importedTask.description)
            assertEquals(tasks[index].priority, importedTask.priority)
            assertEquals(tasks[index].status, importedTask.status)
            assertEquals(tasks[index].deadline, importedTask.deadline)
        }
    }

    @Test
    fun `JSON export and import round trip preserves data`() {
        val tasks = createSampleTasks()

        val json = jsonExporter.export(tasks)
        val imported = jsonImporter.import(json)

        assertEquals(tasks.size, imported.size)
        for ((index, result) in imported.withIndex()) {
            assertTrue(result.isSuccess)
            val importedTask = result.getOrThrow()
            assertEquals(tasks[index].title, importedTask.title)
            assertEquals(tasks[index].description, importedTask.description)
            assertEquals(tasks[index].priority, importedTask.priority)
            assertEquals(tasks[index].status, importedTask.status)
            assertEquals(tasks[index].deadline, importedTask.deadline)
            assertEquals(tasks[index].createdAt, importedTask.createdAt)
            assertEquals(tasks[index].completedAt, importedTask.completedAt)
        }
    }

    @Test
    fun `CSV export handles commas in fields`() {
        val tasks = listOf(
            TaskEntity(
                title = "Task with, comma",
                description = "Description with, commas, and more",
                priority = "HIGH",
                status = "PENDING",
                createdAt = 1000L
            )
        )

        val csv = csvExporter.export(tasks)
        val imported = csvImporter.import(csv)

        assertEquals(1, imported.size)
        assertTrue(imported[0].isSuccess)
        assertEquals("Task with, comma", imported[0].getOrThrow().title)
        assertEquals("Description with, commas, and more", imported[0].getOrThrow().description)
    }

    @Test
    fun `CSV export handles quotes in fields`() {
        val tasks = listOf(
            TaskEntity(
                title = "Task with \"quotes\"",
                description = "Normal description",
                priority = "MEDIUM",
                status = "PENDING",
                createdAt = 2000L
            )
        )

        val csv = csvExporter.export(tasks)
        val imported = csvImporter.import(csv)

        assertEquals(1, imported.size)
        assertTrue(imported[0].isSuccess)
        assertEquals("Task with \"quotes\"", imported[0].getOrThrow().title)
    }

    @Test
    fun `JSON export produces valid JSON`() {
        val tasks = createSampleTasks()
        val json = jsonExporter.export(tasks)

        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"title\""))
        assertTrue(json.contains("\"priority\""))
    }

    @Test
    fun `CSV import handles missing optional fields`() {
        val csv = "title,description,priority\nTest Task,Description,HIGH"
        val imported = csvImporter.import(csv)

        assertEquals(1, imported.size)
        assertTrue(imported[0].isSuccess)
        assertEquals("Test Task", imported[0].getOrThrow().title)
        assertEquals("HIGH", imported[0].getOrThrow().priority)
    }

    @Test
    fun `JSON import handles invalid JSON gracefully`() {
        val invalid = "this is not json"
        val imported = jsonImporter.import(invalid)

        assertEquals(1, imported.size)
        assertTrue(imported[0].isFailure)
    }

    @Test
    fun `CSV import rejects lines with too few fields`() {
        val csv = "title,description,priority\nOnly Title,Missing"
        val imported = csvImporter.import(csv)

        assertEquals(1, imported.size)
        assertTrue(imported[0].isFailure)
    }

    @Test
    fun `export empty list produces header only for CSV`() {
        val csv = csvExporter.export(emptyList())
        val lines = csv.lines().filter { it.isNotBlank() }
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("title"))
    }

    @Test
    fun `export empty list produces empty array for JSON`() {
        val json = jsonExporter.export(emptyList())
        assertEquals("[\n]", json.trim())
    }

    private fun createSampleTasks(): List<TaskEntity> {
        return listOf(
            TaskEntity(
                title = "Review PR",
                description = "Check code changes",
                priority = "HIGH",
                status = "PENDING",
                reminderMode = "NORMAL",
                deadline = 1700000000000L,
                createdAt = 1699900000000L,
                completedAt = null
            ),
            TaskEntity(
                title = "Deploy app",
                description = "Push to production",
                priority = "CRITICAL",
                status = "COMPLETED",
                reminderMode = "AGGRESSIVE",
                deadline = 1699950000000L,
                createdAt = 1699800000000L,
                completedAt = 1699940000000L
            ),
            TaskEntity(
                title = "Write docs",
                description = "",
                priority = "LOW",
                status = "PENDING",
                reminderMode = "GENTLE",
                deadline = null,
                createdAt = 1699700000000L,
                completedAt = null
            )
        )
    }
}
