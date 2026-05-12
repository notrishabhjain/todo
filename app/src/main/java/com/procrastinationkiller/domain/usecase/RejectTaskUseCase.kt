package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.domain.model.TaskSuggestion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RejectTaskUseCase @Inject constructor(
    private val learningDataDao: LearningDataDao
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

        return RejectionResult(
            rejectedTitle = suggestion.suggestedTitle,
            sourceApp = suggestion.sourceApp,
            sender = suggestion.sender
        )
    }
}

data class RejectionResult(
    val rejectedTitle: String,
    val sourceApp: String,
    val sender: String
)
