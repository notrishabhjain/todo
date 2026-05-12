package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.domain.engine.learning.LearningEngine
import com.procrastinationkiller.domain.engine.learning.LearningEvent
import com.procrastinationkiller.domain.engine.learning.UserFeedbackType
import com.procrastinationkiller.domain.model.TaskSuggestion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RejectTaskUseCase @Inject constructor(
    private val learningDataDao: LearningDataDao,
    private val learningEngine: LearningEngine? = null
) {

    suspend operator fun invoke(suggestion: TaskSuggestion): RejectionResult {
        val featureVector = buildString {
            append("title=${suggestion.suggestedTitle}")
            append("|source=${suggestion.sourceApp}")
            append("|sender=${suggestion.sender}")
            append("|priority=${suggestion.priority.name}")
            append("|confidence=${suggestion.confidence}")
        }

        val learningData = LearningDataEntity(
            featureVector = featureVector,
            label = "REJECTED",
            confidence = suggestion.confidence,
            timestamp = System.currentTimeMillis()
        )
        learningDataDao.insertLearningData(learningData)

        // Record structured learning feedback
        learningEngine?.recordFeedback(
            LearningEvent(
                feedbackType = UserFeedbackType.REJECTED,
                originalText = suggestion.originalText,
                sourceApp = suggestion.sourceApp,
                sender = suggestion.sender,
                suggestedPriority = suggestion.priority,
                keywords = extractKeywords(suggestion.originalText),
                confidence = suggestion.confidence
            )
        )

        return RejectionResult(
            rejectedTitle = suggestion.suggestedTitle,
            sourceApp = suggestion.sourceApp,
            sender = suggestion.sender
        )
    }

    private fun extractKeywords(text: String): List<String> {
        return text.lowercase()
            .split("\\s+".toRegex())
            .filter { it.length > 3 && it !in STOP_WORDS }
            .take(10)
    }

    companion object {
        private val STOP_WORDS = setOf(
            "this", "that", "with", "from", "have", "been", "were", "they",
            "them", "then", "than", "these", "those", "their", "there",
            "when", "what", "which", "where", "will", "would", "could",
            "should", "about", "after", "before", "between", "each",
            "every", "into", "through", "does", "done", "just", "more",
            "most", "much", "also", "back", "some", "such", "very",
            "your", "yours", "here", "only", "still", "over", "under"
        )
    }
}

data class RejectionResult(
    val rejectedTitle: String,
    val sourceApp: String,
    val sender: String
)
