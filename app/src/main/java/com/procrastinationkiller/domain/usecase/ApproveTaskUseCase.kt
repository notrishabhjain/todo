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
                keywords = TextKeywordExtractor.extractKeywords(suggestion.originalText),
                confidence = suggestion.confidence
            )
        )

        return taskId
    }
}