package com.procrastinationkiller.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.procrastinationkiller.data.export.ExportFormat
import com.procrastinationkiller.data.export.ExportImportService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class AutoExportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val exportImportService: ExportImportService
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "auto_export_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoExportWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val result = exportImportService.exportTasks(ExportFormat.JSON)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val fileName = "tasks_backup_${dateFormat.format(Date())}.json"

            val exportDir = File(applicationContext.filesDir, "exports")
            exportDir.mkdirs()
            val file = File(exportDir, fileName)
            file.writeText(result.content)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
