package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.engine.learning.LearningEngine
import com.procrastinationkiller.domain.engine.learning.LearningEvent
import com.procrastinationkiller.domain.engine.learning.UserFeedbackType
import com.procrastinationkiller.domain.model.TaskSuggestion
import com.procrastinationkiller.domain.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApproveTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val learningEngine: LearningEngine? = null
) {

    suspend operator fun invoke(suggestion: TaskSuggestion): Long {
        val taskEntity = TaskEntity(
            title = suggestion.suggestedTitle,
            description = suggestion.description,
            priority = suggestion.priority.name,
            status = "PENDING",
            deadline = suggestion.dueDate,
            createdAt = System.currentTimeMillis()
        )
        val taskId = taskRepository.insertTask(taskEntity)

        // Record learning feedback
        learningEngine?.recordFeedback(
            LearningEvent(
                feedbackType = UserFeedbackType.APPROVED,
                originalText = suggestion.originalText,
                sourceApp = suggestion.sourceApp,
                sender = suggestion.sender,
                suggestedPriority = suggestion.priority,
                keywords = extractKeywords(suggestion.originalText),
                confidence = suggestion.confidence
            )
        )

        return taskId
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