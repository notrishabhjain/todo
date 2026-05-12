package com.procrastinationkiller.domain.engine.learning

import com.procrastinationkiller.domain.model.TaskPriority

data class LearningEvent(
    val feedbackType: UserFeedbackType,
    val originalText: String,
    val sourceApp: String,
    val sender: String,
    val suggestedPriority: TaskPriority,
    val finalPriority: TaskPriority? = null,
    val keywords: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 0f
)
