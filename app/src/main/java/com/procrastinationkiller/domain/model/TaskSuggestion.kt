package com.procrastinationkiller.domain.model

import com.procrastinationkiller.domain.engine.whatsapp.WhatsAppContext

data class TaskSuggestion(
    val id: Long = 0,
    val suggestedTitle: String,
    val description: String,
    val priority: TaskPriority,
    val dueDate: Long?,
    val sourceApp: String,
    val sender: String,
    val originalText: String,
    val confidence: Float,
    val whatsAppContext: WhatsAppContext? = null,
    val autoApprove: Boolean = false,
    val contactPriority: ContactPriority? = null
)
