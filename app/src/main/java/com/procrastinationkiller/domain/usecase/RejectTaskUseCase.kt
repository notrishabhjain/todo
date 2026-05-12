package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.domain.model.TaskSuggestion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RejectTaskUseCase @Inject constructor() {

    suspend operator fun invoke(suggestion: TaskSuggestion): RejectionResult {
        // Store feedback for learning - in production this would persist to a learning data store
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
