package com.procrastinationkiller.domain.engine.learning

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.domain.model.TaskPriority
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearningEngine @Inject constructor(
    private val learningDataDao: LearningDataDao,
    private val adaptiveWeightManager: AdaptiveWeightManager
) {

    companion object {
        private const val MAX_RECORDS = 1000
        private const val AUTO_APPROVE_SENDER_THRESHOLD = 1.8f
        private const val AUTO_APPROVE_APP_THRESHOLD = 1.8f
    }

    suspend fun recordFeedback(event: LearningEvent) {
        // Update in-memory weights
        adaptiveWeightManager.updateFromFeedback(event)

        // Persist to database
        val entity = LearningDataEntity(
            featureVector = buildFeatureVector(event),
            label = event.feedbackType.name,
            confidence = event.confidence,
            timestamp = event.timestamp,
            feedbackType = event.feedbackType.name,
            sourceApp = event.sourceApp,
            sender = event.sender,
            keywords = event.keywords.joinToString(","),
            prioritySuggested = event.suggestedPriority.name,
            priorityFinal = event.finalPriority?.name
        )
        learningDataDao.insertLearningData(entity)

        // Prune old records if over limit
        pruneOldRecords()
    }

    fun getAdaptedAnalysis(
        text: String,
        sender: String,
        sourceApp: String
    ): LearningAdjustment {
        val senderImportance = adaptiveWeightManager.getSenderImportance(sender)
        val appReliability = adaptiveWeightManager.getAppReliability(sourceApp)

        // Calculate confidence boost from sender and app trust
        val confidenceBoost = calculateConfidenceBoost(senderImportance, appReliability)

        // Determine if priority should be adjusted based on learning
        val priorityAdjustment = determinePriorityAdjustment(senderImportance)

        // Determine if auto-approve is warranted
        val shouldAutoApprove = senderImportance >= AUTO_APPROVE_SENDER_THRESHOLD &&
            appReliability >= AUTO_APPROVE_APP_THRESHOLD

        return LearningAdjustment(
            confidenceBoost = confidenceBoost,
            priorityAdjustment = priorityAdjustment,
            shouldAutoApprove = shouldAutoApprove
        )
    }

    private fun calculateConfidenceBoost(
        senderImportance: Float,
        appReliability: Float
    ): Float {
        // Boost is proportional to trust in sender and app
        val senderBoost = (senderImportance - 1.0f) * 0.1f
        val appBoost = (appReliability - 1.0f) * 0.05f
        return (senderBoost + appBoost).coerceIn(-0.2f, 0.3f)
    }

    private fun determinePriorityAdjustment(senderImportance: Float): TaskPriority? {
        // If sender is very important, suggest higher priority
        return when {
            senderImportance >= 2.5f -> TaskPriority.CRITICAL
            senderImportance >= 2.0f -> TaskPriority.HIGH
            else -> null
        }
    }

    private suspend fun pruneOldRecords() {
        learningDataDao.deleteOldest(MAX_RECORDS)
    }

    private fun buildFeatureVector(event: LearningEvent): String {
        return buildString {
            append("type=${event.feedbackType.name}")
            append("|source=${event.sourceApp}")
            append("|sender=${event.sender}")
            append("|priority=${event.suggestedPriority.name}")
            append("|confidence=${event.confidence}")
            append("|keywords=${event.keywords.joinToString(",")}")
        }
    }
}
