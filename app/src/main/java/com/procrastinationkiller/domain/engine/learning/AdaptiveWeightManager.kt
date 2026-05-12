package com.procrastinationkiller.domain.engine.learning

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveWeightManager @Inject constructor(
    private val learningDataDao: LearningDataDao
) {

    companion object {
        private const val DEFAULT_WEIGHT = 1.0f
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.5f
        private const val EMA_ALPHA = 0.3f
        private const val MIN_WEIGHT = 0.1f
        private const val MAX_WEIGHT = 3.0f
        private const val MAX_CONFIDENCE_THRESHOLD = 0.9f
        private const val MIN_CONFIDENCE_THRESHOLD = 0.3f
    }

    // Thread-safe flag indicating whether initial load from DB has completed.
    // Defaults to true so the manager is usable with defaults; set to false during loadFromDatabase.
    private val loadingComplete = AtomicBoolean(true)

    // Thread-safe in-memory caches for fast access
    private val keywordWeights = ConcurrentHashMap<String, Float>()
    private val senderImportance = ConcurrentHashMap<String, Float>()
    private val appReliability = ConcurrentHashMap<String, Float>()
    @Volatile
    private var confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD

    fun isLoadingComplete(): Boolean = loadingComplete.get()

    fun getKeywordBoost(keyword: String): Float {
        if (!loadingComplete.get()) return DEFAULT_WEIGHT
        return keywordWeights[keyword.lowercase()] ?: DEFAULT_WEIGHT
    }

    fun getSenderImportance(sender: String): Float {
        if (!loadingComplete.get()) return DEFAULT_WEIGHT
        return senderImportance[sender.lowercase()] ?: DEFAULT_WEIGHT
    }

    fun getAppReliability(app: String): Float {
        if (!loadingComplete.get()) return DEFAULT_WEIGHT
        return appReliability[app.lowercase()] ?: DEFAULT_WEIGHT
    }

    fun getConfidenceThreshold(): Float {
        if (!loadingComplete.get()) return DEFAULT_CONFIDENCE_THRESHOLD
        return confidenceThreshold
    }

    fun updateFromFeedback(event: LearningEvent) {
        val direction = event.feedbackType.weightDirection

        // Update keyword weights
        for (keyword in event.keywords) {
            val key = keyword.lowercase()
            val current = keywordWeights[key] ?: DEFAULT_WEIGHT
            val updated = exponentialMovingAverage(current, direction)
            keywordWeights[key] = updated.coerceIn(MIN_WEIGHT, MAX_WEIGHT)
        }

        // Update sender importance
        if (event.sender.isNotBlank()) {
            val senderKey = event.sender.lowercase()
            val currentSender = senderImportance[senderKey] ?: DEFAULT_WEIGHT
            val updatedSender = exponentialMovingAverage(currentSender, direction)
            senderImportance[senderKey] = updatedSender.coerceIn(MIN_WEIGHT, MAX_WEIGHT)
        }

        // Update app reliability
        if (event.sourceApp.isNotBlank()) {
            val appKey = event.sourceApp.lowercase()
            val currentApp = appReliability[appKey] ?: DEFAULT_WEIGHT
            val updatedApp = exponentialMovingAverage(currentApp, direction)
            appReliability[appKey] = updatedApp.coerceIn(MIN_WEIGHT, MAX_WEIGHT)
        }

        // Adjust confidence threshold based on rejections
        when (event.feedbackType) {
            UserFeedbackType.REJECTED, UserFeedbackType.IGNORED -> {
                confidenceThreshold = (confidenceThreshold + 0.02f)
                    .coerceAtMost(MAX_CONFIDENCE_THRESHOLD)
            }
            UserFeedbackType.APPROVED, UserFeedbackType.COMPLETED_QUICKLY -> {
                confidenceThreshold = (confidenceThreshold - 0.01f)
                    .coerceAtLeast(MIN_CONFIDENCE_THRESHOLD)
            }
            else -> { /* no change */ }
        }
    }

    suspend fun loadFromDatabase() {
        loadingComplete.set(false)
        val recentData = learningDataDao.getRecentDataList(1000)
        keywordWeights.clear()
        senderImportance.clear()
        appReliability.clear()
        confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD

        // Apply decay: older events have less impact
        val now = System.currentTimeMillis()
        val decayPeriodMs = 30L * 24 * 60 * 60 * 1000 // 30 days

        for (entity in recentData) {
            val age = now - entity.timestamp
            val decayFactor = maxOf(0.1f, 1.0f - (age.toFloat() / decayPeriodMs.toFloat()))

            val feedbackType = try {
                UserFeedbackType.valueOf(entity.feedbackType ?: "APPROVED")
            } catch (_: IllegalArgumentException) {
                if (entity.label == "REJECTED") UserFeedbackType.REJECTED
                else UserFeedbackType.APPROVED
            }

            val direction = feedbackType.weightDirection * decayFactor

            // Rebuild keyword weights
            val keywords = entity.keywords?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            for (keyword in keywords) {
                val key = keyword.lowercase().trim()
                val current = keywordWeights[key] ?: DEFAULT_WEIGHT
                keywordWeights[key] = exponentialMovingAverage(current, direction)
                    .coerceIn(MIN_WEIGHT, MAX_WEIGHT)
            }

            // Rebuild sender importance
            val sender = entity.sender
            if (!sender.isNullOrBlank()) {
                val senderKey = sender.lowercase()
                val current = senderImportance[senderKey] ?: DEFAULT_WEIGHT
                senderImportance[senderKey] = exponentialMovingAverage(current, direction)
                    .coerceIn(MIN_WEIGHT, MAX_WEIGHT)
            }

            // Rebuild app reliability
            val app = entity.sourceApp
            if (!app.isNullOrBlank()) {
                val appKey = app.lowercase()
                val current = appReliability[appKey] ?: DEFAULT_WEIGHT
                appReliability[appKey] = exponentialMovingAverage(current, direction)
                    .coerceIn(MIN_WEIGHT, MAX_WEIGHT)
            }

            // Rebuild confidence threshold
            when (feedbackType) {
                UserFeedbackType.REJECTED, UserFeedbackType.IGNORED -> {
                    confidenceThreshold = (confidenceThreshold + 0.02f * decayFactor)
                        .coerceAtMost(MAX_CONFIDENCE_THRESHOLD)
                }
                UserFeedbackType.APPROVED, UserFeedbackType.COMPLETED_QUICKLY -> {
                    confidenceThreshold = (confidenceThreshold - 0.01f * decayFactor)
                        .coerceAtLeast(MIN_CONFIDENCE_THRESHOLD)
                }
                else -> { /* no change */ }
            }
        }

        loadingComplete.set(true)
    }

    private fun exponentialMovingAverage(current: Float, direction: Float): Float {
        val target = current + direction * 0.2f
        return current * (1 - EMA_ALPHA) + target * EMA_ALPHA
    }
}
