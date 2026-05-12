package com.procrastinationkiller.domain.usecase

import android.service.notification.StatusBarNotification
import com.procrastinationkiller.data.local.dao.TaskSuggestionDao
import com.procrastinationkiller.data.local.entity.NotificationEntity
import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity
import com.procrastinationkiller.data.parser.NotificationParser
import com.procrastinationkiller.data.parser.WhatsAppHandler
import com.procrastinationkiller.domain.engine.TaskExtractionEngine
import com.procrastinationkiller.domain.engine.whatsapp.WhatsAppEvaluationResult
import com.procrastinationkiller.domain.engine.whatsapp.WhatsAppIntelligenceEngine
import com.procrastinationkiller.domain.model.TaskPriority
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
    private val taskSuggestionDao: TaskSuggestionDao,
    private val whatsAppIntelligenceEngine: WhatsAppIntelligenceEngine? = null
) {

    suspend fun processNotification(sbn: StatusBarNotification): TaskSuggestion? {
        val parsed = notificationParser.parse(sbn)

        // Get the message text to analyze
        val textToAnalyze: String
        val sender: String
        var whatsAppEvalResult: WhatsAppEvaluationResult.ProcessResult? = null

        if (whatsAppHandler.isWhatsAppNotification(parsed.packageName)) {
            val whatsAppMessage = whatsAppHandler.parseWhatsAppNotification(sbn)
            textToAnalyze = whatsAppHandler.getMessageContent(whatsAppMessage)
            sender = whatsAppMessage.sender

            // Use WhatsApp intelligence engine if available
            if (whatsAppIntelligenceEngine != null) {
                val evalResult = whatsAppIntelligenceEngine.evaluate(
                    sender = whatsAppMessage.sender,
                    message = textToAnalyze,
                    isGroupChat = whatsAppMessage.isGroupChat,
                    groupName = whatsAppMessage.groupName,
                    timestamp = whatsAppMessage.timestamp
                )

                when (evalResult) {
                    is WhatsAppEvaluationResult.IgnoreResult -> {
                        // Store notification but skip task creation
                        val notificationEntity = NotificationEntity(
                            packageName = parsed.packageName,
                            title = parsed.title,
                            content = parsed.text,
                            timestamp = parsed.timestamp,
                            isProcessed = true
                        )
                        notificationRepository.insertNotification(notificationEntity)
                        return null
                    }
                    is WhatsAppEvaluationResult.ProcessResult -> {
                        whatsAppEvalResult = evalResult
                    }
                }
            }
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
        var suggestion = taskExtractionEngine.extract(
            text = textToAnalyze,
            sourceApp = parsed.packageName,
            sender = sender
        )

        // Apply WhatsApp intelligence enhancements
        if (suggestion != null && whatsAppEvalResult != null) {
            val override = whatsAppEvalResult.priorityOverride
            val priority = if (override != null && override.ordinal < suggestion.priority.ordinal) {
                override
            } else {
                suggestion.priority
            }

            suggestion = suggestion.copy(
                priority = priority,
                whatsAppContext = whatsAppEvalResult.whatsAppContext,
                autoApprove = whatsAppEvalResult.autoApprove,
                contactPriority = whatsAppEvalResult.contactPriority
            )
        }

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
                confidence = suggestion.confidence,
                autoApprove = suggestion.autoApprove
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
