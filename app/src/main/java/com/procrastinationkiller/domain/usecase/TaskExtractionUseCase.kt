package com.procrastinationkiller.domain.usecase

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import com.procrastinationkiller.presentation.MainActivity
import com.procrastinationkiller.service.NotificationChannelManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
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
    @ApplicationContext private val context: Context,
    private val notificationDao: com.procrastinationkiller.data.local.dao.NotificationDao,
    private val whatsAppIntelligenceEngine: WhatsAppIntelligenceEngine? = null,
    private val semanticDeduplicator: SemanticDeduplicator? = null,
    private val approveTaskUseCase: ApproveTaskUseCase? = null
) {

    companion object {
        private const val SUGGESTION_NOTIFICATION_ID_BASE = 3000
        private const val SUGGESTION_NOTIFICATION_ID_LIMIT = 4000

        // Monotonically incrementing counter for unique notification IDs.
        // Cycles through [3000, 4000) providing 1000 unique IDs before wrapping.
        private val notificationIdCounter = AtomicInteger(SUGGESTION_NOTIFICATION_ID_BASE)

        private fun nextNotificationId(): Int {
            val id = notificationIdCounter.getAndIncrement()
            if (id >= SUGGESTION_NOTIFICATION_ID_LIMIT) {
                notificationIdCounter.compareAndSet(id + 1, SUGGESTION_NOTIFICATION_ID_BASE)
                return SUGGESTION_NOTIFICATION_ID_BASE
            }
            return id
        }

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

    private fun postSuggestionNotification(title: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = nextNotificationId()
        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_SUGGESTIONS)
            .setContentTitle("New Task Suggestion")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }

    suspend fun processNotification(sbn: StatusBarNotification, sbnKey: String? = null): TaskSuggestion? {
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
                category = parsed.category,
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
                    notificationKey = notificationKey,
                    sbnKey = sbnKey
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
                postSuggestionNotification(suggestion.suggestedTitle)

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
                            notificationKey = notificationKey,
                            sbnKey = sbnKey
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

        // sbnKey-based dedup: VIP uses 5-second window (prevent double-fire), others use 1-hour window
        val isVipAutoApprove = whatsAppEvalResult?.autoApprove == true
        if (sbnKey != null) {
            val dedupWindow = if (isVipAutoApprove) 5_000L else 3_600_000L
            val windowStart = System.currentTimeMillis() - dedupWindow
            val count = notificationDao.countBySbnKeyInLastHour(sbnKey, windowStart)
            if (count > 0) {
                return null
            }
        }

        // Store notification in DB
        val notificationEntity = NotificationEntity(
            packageName = parsed.packageName,
            title = parsed.title,
            content = parsed.text,
            timestamp = parsed.timestamp,
            isProcessed = false,
            notificationKey = notificationKey,
            sbnKey = sbnKey
        )
        val notificationId = notificationRepository.insertNotification(notificationEntity)

        // WhatsApp multi-message: if WhatsApp text contains newlines, split and process each line
        if (whatsAppHandler.isWhatsAppNotification(parsed.packageName) && textToAnalyze.contains("\n")) {
            val lines = textToAnalyze.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            if (lines.size > 1) {
                var lastSuggestion: TaskSuggestion? = null
                for (line in lines) {
                    val lineSuggestion = processWhatsAppLine(
                        line = line,
                        sender = sender,
                        packageName = parsed.packageName,
                        whatsAppEvalResult = whatsAppEvalResult,
                        notificationId = notificationId,
                        notificationEntity = notificationEntity
                    )
                    if (lineSuggestion != null) {
                        lastSuggestion = lineSuggestion
                    }
                }
                // Mark notification as processed
                notificationRepository.updateNotification(
                    notificationEntity.copy(id = notificationId, isProcessed = true)
                )
                return lastSuggestion
            }
        }

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

        // VIP FORCE-CREATE: If VIP contact's message didn't produce a suggestion via keyword/ML,
        // create one anyway using the raw message text
        if (suggestion == null && whatsAppEvalResult?.autoApprove == true) {
            suggestion = TaskSuggestion(
                suggestedTitle = textToAnalyze.take(60),
                description = textToAnalyze,
                priority = TaskPriority.HIGH,
                dueDate = null,
                sourceApp = parsed.packageName,
                sender = sender,
                originalText = textToAnalyze,
                confidence = 1.0f,
                autoApprove = true,
                whatsAppContext = whatsAppEvalResult.whatsAppContext,
                contactPriority = whatsAppEvalResult.contactPriority
            )
        }

        // Persist the suggestion to the database if one was extracted
        if (suggestion != null) {
            // Check for duplicates using content hash (covers ALL statuses).
            val contentHash = computeContentHash(suggestion.sender, suggestion.originalText, suggestion.sourceApp)
            val existingByHash = taskSuggestionDao.findByContentHash(contentHash)
            if (existingByHash != null) {
                // Duplicate content already exists - skip
                notificationRepository.updateNotification(
                    notificationEntity.copy(id = notificationId, isProcessed = true)
                )
                return null
            }

            // Semantic deduplication: skip entirely for VIP auto-approve messages.
            // Only exact content hash dedup (above) should remain for VIP.
            if (suggestion.autoApprove != true && semanticDeduplicator != null) {
                val tenMinutesAgo = System.currentTimeMillis() - 600_000L
                val recentSuggestions = taskSuggestionDao.getAllRecentSuggestions(tenMinutesAgo)
                val deduplicationResult = semanticDeduplicator.checkDuplicate(
                    newText = suggestion.suggestedTitle,
                    newSender = suggestion.sender,
                    existingSuggestions = recentSuggestions
                )

                if (deduplicationResult.isDuplicate && deduplicationResult.existingTaskId != null) {
                    // If new message has higher priority, update existing
                    val existingEntity = recentSuggestions.find { it.id == deduplicationResult.existingTaskId }
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
                    notificationRepository.updateNotification(
                        notificationEntity.copy(id = notificationId, isProcessed = true)
                    )
                    return null
                }
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
                taskSuggestionDao.insert(entity)
                postSuggestionNotification(suggestion.suggestedTitle)
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

    private suspend fun processWhatsAppLine(
        line: String,
        sender: String,
        packageName: String,
        whatsAppEvalResult: WhatsAppEvaluationResult.ProcessResult?,
        notificationId: Long,
        notificationEntity: NotificationEntity
    ): TaskSuggestion? {
        var suggestion = taskExtractionEngine.extract(
            text = line,
            sourceApp = packageName,
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

        // VIP FORCE-CREATE: If VIP contact's message line didn't produce a suggestion,
        // create one anyway using the raw line text
        if (suggestion == null && whatsAppEvalResult?.autoApprove == true) {
            suggestion = TaskSuggestion(
                suggestedTitle = line.take(60),
                description = line,
                priority = TaskPriority.HIGH,
                dueDate = null,
                sourceApp = packageName,
                sender = sender,
                originalText = line,
                confidence = 1.0f,
                autoApprove = true,
                whatsAppContext = whatsAppEvalResult.whatsAppContext,
                contactPriority = whatsAppEvalResult.contactPriority
            )
        }

        if (suggestion == null) return null

        val contentHash = computeContentHash(suggestion.sender, suggestion.originalText, suggestion.sourceApp)
        val existingByHash = taskSuggestionDao.findByContentHash(contentHash)
        if (existingByHash != null) {
            return null
        }

        // Skip semantic dedup for VIP auto-approve messages
        if (suggestion.autoApprove != true && semanticDeduplicator != null) {
            val tenMinutesAgo = System.currentTimeMillis() - 600_000L
            val recentSuggestions = taskSuggestionDao.getAllRecentSuggestions(tenMinutesAgo)
            val deduplicationResult = semanticDeduplicator.checkDuplicate(
                newText = suggestion.suggestedTitle,
                newSender = suggestion.sender,
                existingSuggestions = recentSuggestions
            )
            if (deduplicationResult.isDuplicate) {
                return null
            }
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

        if (suggestion.autoApprove && approveTaskUseCase != null) {
            val approvedEntity = entity.copy(status = "APPROVED")
            taskSuggestionDao.insert(approvedEntity)
            approveTaskUseCase.invoke(suggestion)
        } else {
            taskSuggestionDao.insert(entity)
            postSuggestionNotification(suggestion.suggestedTitle)
        }

        return suggestion
    }
}
