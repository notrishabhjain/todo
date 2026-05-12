package com.procrastinationkiller.domain.engine.ml

import com.procrastinationkiller.domain.model.TaskPriority

enum class IntentType {
    TASK_REQUEST,
    INFORMATION,
    SOCIAL,
    QUESTION,
    REMINDER,
    MEETING
}

data class ClassificationResult(
    val intent: IntentType,
    val confidence: Float,
    val isActionable: Boolean,
    val suggestedPriority: TaskPriority?
)

interface IntentClassifier {
    fun classify(features: FloatArray): ClassificationResult?
}
