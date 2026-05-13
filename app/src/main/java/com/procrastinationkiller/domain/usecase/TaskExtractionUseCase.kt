package com.procrastinationkiller.domain.usecase

import android.service.notification.StatusBarNotification
import com.procrastinationkiller.data.local.dao.TaskSuggestionDao
import com.procrastinationkiller.data.local.entity.NotificationEntity
import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity
import com.procrastinationkiller.data.parser.AppType
import com.procrastinationkiller.data.parser.NotificationParser
import com.procrastinationkiller.data.parser.PhoneCallHandler
import com.procrastinationkiller.data.parser.WhatsAppHandler
import com.procrastinationkiller.domain.engine.TaskExtractionEngine
import com.procrastinationkiller.domain.engine.semantic.SemanticDeduplicator
import com.procrastinationkiller.domain.engine.whatsapp.WhatsAppEvaluationResult
import com.procrastinationkiller.domain.engine.whatsapp.WhatsAppIntelligenceEngine
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskSuggestion
import com.procrastinationkiller.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskExtractionUseCase @Inject constructor(
    private val notificationParser: NotificationParser,
    private val whatsAppHandler: WhatsAppHandler,
    private val phoneCallHandler: PhoneCallHandler,
    private val taskExtractionEngine: TaskExtractionEngine,
    private val notificationRepository: NotificationRepository,
    private val taskSuggestionDao: TaskSuggestionDao,
    private val whatsAppIntelligenceEngine: WhatsAppIntelligenceEngine? = null,
    private val semanticDeduplicator: SemanticDeduplicator? = null,
    private val approveTaskUseCase: ApproveTaskUseCase? = null
) {

    suspend fun processNotification(sbn: StatusBarNotification): TaskSuggestion? {
        val parsed = notificationParser.parse(sbn)

        // Check for duplicate notification by key (packageName + title + content)
        val notificationKey = computeNotificationKey(parsed.packageName, parsed.title, parsed.text)
        val alreadyProcessedCount = notificationRepository.countProcessedByKey(notificationKey)
        if (alreadyProcessedCount > 0) {
            return null
        }

        // Handle missed call notifications from phone dialer
        if (parsed.appType == AppType.PHONE_DIALER) {
            val missedCall = phoneCallHandler.detectMissedCallFromParsed(
                packageName = parsed.packageName,
                title = parsed.title,
                text = parsed.text,
                category = null,
                timestamp = parsed.timestamp
            )
            if (missedCall != null) {
                val taskTitle = phoneCallHandler.generateTaskTitle(missedCall.callerName)
                val suggestion = TaskSuggestion(
                    suggestedTitle = taskTitle,
                    description = "Missed call from ${missedCall.callerName}",
                    priority = TaskPriority.HIGH,
                    dueDate = null,
                    sourceApp = parsed.packageName,
                    sender = missedCall.callerName,
                    originalText = "${parsed.title} ${parsed.text}".trim(),
                    confidence = 0.9f,
                    autoApprove = false
                )

                // Check for duplicates using content hash
                val contentHash = computeContentHash(suggestion.sender, suggestion.originalText, suggestion.sourceApp)
                val existingByHash = taskSuggestionDao.findByContentHash(contentHash)
                if (existingByHash != null) {
                    return null
                }

                // Store notification
                val notificationEntity = NotificationEntity(
                    packageName = parsed.packageName,
                    title = parsed.title,
                    content = parsed.text,
                    timestamp = parsed.timestamp,
                    isProcessed = true,
                    notificationKey = notificationKey
                )
                notificationRepository.insertNotification(notificationEntity)

                // Persist suggestion
                val entity = TaskSuggestionEntity(
                    suggestedTitle = suggestion.suggestedTitle,
                    description = suggestion.description,
                    priority = suggestion.priority.name,
                    dueDate = suggestion.dueDate,
                    sourceApp = suggestion.sourceApp,
                    sender = suggestion.sender,
                    originalText = suggestion.originalText,
                    confidence = suggestion.confidence,
                    autoApprove = suggestion.autoApprove,
                    contentHash = contentHash
                )
                taskSuggestionDao.insert(entity)

                return suggestion
            }
            // Not a missed call - skip non-missed-call dialer notifications
            return null
        }

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
                            isProcessed = true,
                            notificationKey = notificationKey
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
            isProcessed = false,
            notificationKey = notificationKey
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
            val priority = if (override != null && override.ordinal > suggestion.priority.ordinal) {
                override
            } else {
                suggestion.priority
            }

            suggestion = suggestion.copy(
                priority = priority,
                whatsAppContext = whatsAppEvalResult.whatsAppContext,
                autoApprove = suggestion.autoApprove || whatsAppEvalResult.autoApprove,
                contactPriority = whatsAppEvalResult.contactPriority
            )
        }

        // Persist the suggestion to the database if one was extracted
        if (suggestion != null) {
            // Check for duplicates using content hash (covers ALL statuses)
            val contentHash = computeContentHash(suggestion.sender, suggestion.originalText, suggestion.sourceApp)
            val existingByHash = taskSuggestionDao.findByContentHash(contentHash)
            if (existingByHash != null) {
                // Duplicate content already exists - skip
                notificationRepository.updateNotification(
                    notificationEntity.copy(id = notificationId, isProcessed = true)
                )
                return null
            }

            val entity = TaskSuggestionEntity(
                suggestedTitle = suggestion.suggestedTitle,
                description = suggestion.description,
                priority = suggestion.priority.name,
                dueDate = suggestion.dueDate,
                sourceApp = suggestion.sourceApp,
                sender = suggestion.sender,
                originalText = suggestion.originalText,
                confidence = suggestion.confidence,
                autoApprove = suggestion.autoApprove,
                contentHash = contentHash
            )

            // VIP auto-approve: directly create the task without going through the inbox
            if (suggestion.autoApprove && approveTaskUseCase != null) {
                val approvedEntity = entity.copy(status = "APPROVED")
                taskSuggestionDao.insert(approvedEntity)
                approveTaskUseCase.invoke(suggestion)
            } else {
                // Check for duplicates using semantic deduplication
                if (semanticDeduplicator != null) {
                    val pendingSuggestions = taskSuggestionDao.getByStatus("PENDING").first()
                    val deduplicationResult = semanticDeduplicator.checkDuplicate(
                        newText = suggestion.originalText,
                        newSender = suggestion.sender,
                        existingSuggestions = pendingSuggestions
                    )

                    if (deduplicationResult.isDuplicate && deduplicationResult.existingTaskId != null) {
                        // If new message has higher priority, update existing
                        val existingEntity = pendingSuggestions.find { it.id == deduplicationResult.existingTaskId }
                        if (existingEntity != null) {
                            val existingPriority = try {
                                TaskPriority.valueOf(existingEntity.priority)
                            } catch (_: IllegalArgumentException) {
                                TaskPriority.MEDIUM
                            }
                            if (suggestion.priority.ordinal > existingPriority.ordinal) {
                                taskSuggestionDao.insert(
                                    existingEntity.copy(priority = suggestion.priority.name)
                                )
                            }
                        }
                        // Skip insertion for duplicate
                    } else {
                        taskSuggestionDao.insert(entity)
                    }
                } else {
                    taskSuggestionDao.insert(entity)
                }
            }
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

    companion object {
        fun computeContentHash(sender: String, originalText: String, sourceApp: String): String {
            val input = "$sender|$originalText|$sourceApp"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        fun computeNotificationKey(packageName: String, title: String, content: String): String {
            val input = "$packageName|$title|$content"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
