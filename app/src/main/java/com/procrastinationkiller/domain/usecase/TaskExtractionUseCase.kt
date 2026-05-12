package com.procrastinationkiller.domain.usecase

import android.service.notification.StatusBarNotification
import com.procrastinationkiller.data.local.dao.TaskSuggestionDao
import com.procrastinationkiller.data.local.entity.NotificationEntity
import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity
import com.procrastinationkiller.data.parser.NotificationParser
import com.procrastinationkiller.data.parser.WhatsAppHandler
import com.procrastinationkiller.domain.engine.TaskExtractionEngine
import com.procrastinationkiller.domain.model.TaskSuggestion
import com.procrastinationkiller.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskExtractionUseCase @Inject constructor(
    private val notificationParser: NotificationParser,
    private val whatsAppHandler: WhatsAppHandler,
    private val taskExtractionEngine: TaskExtractionEngine,
    private val notificationRepository: NotificationRepository,
    private val taskSuggestionDao: TaskSuggestionDao
) {

    suspend fun processNotification(sbn: StatusBarNotification): TaskSuggestion? {
        val parsed = notificationParser.parse(sbn)

        // Get the message text to analyze
        val textToAnalyze: String
        val sender: String

        if (whatsAppHandler.isWhatsAppNotification(parsed.packageName)) {
            val whatsAppMessage = whatsAppHandler.parseWhatsAppNotification(sbn)
            textToAnalyze = whatsAppHandler.getMessageContent(whatsAppMessage)
            sender = whatsAppMessage.sender
        } else {
            textToAnalyze = parsed.text.ifEmpty { parsed.title }
            sender = parsed.sender
        }

        // Store notification in DB
        val notificationEntity = NotificationEntity(
            packageName = parsed.packageName,
            title = parsed.title,
            content = parsed.text,
            timestamp = parsed.timestamp,
            isProcessed = false
        )
        val notificationId = notificationRepository.insertNotification(notificationEntity)

        // Attempt to extract a task
        val suggestion = taskExtractionEngine.extract(
            text = textToAnalyze,
            sourceApp = parsed.packageName,
            sender = sender
        )

        // Persist the suggestion to the database if one was extracted
        if (suggestion != null) {
            val entity = TaskSuggestionEntity(
                suggestedTitle = suggestion.suggestedTitle,
                description = suggestion.description,
                priority = suggestion.priority.name,
                dueDate = suggestion.dueDate,
                sourceApp = suggestion.sourceApp,
                sender = suggestion.sender,
                originalText = suggestion.originalText,
                confidence = suggestion.confidence
            )
            taskSuggestionDao.insert(entity)
        }

        // Mark notification as processed
        notificationRepository.updateNotification(
            notificationEntity.copy(
                id = notificationId,
                isProcessed = true
            )
        )

        return suggestion
    }
}
