package com.procrastinationkiller.domain.model

data class TaskSuggestion(
    val suggestedTitle: String,
    val description: String,
    val priority: TaskPriority,
    val dueDate: Long?,
    val sourceApp: String,
    val sender: String,
    val originalText: String,
    val confidence: Float,
    val shouldAutoApprove: Boolean = false
)
