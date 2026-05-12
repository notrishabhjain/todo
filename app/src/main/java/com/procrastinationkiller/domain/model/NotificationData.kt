package com.procrastinationkiller.domain.model

data class NotificationData(
    val id: Long = 0,
    val packageName: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false,
    val extractedTaskId: Long? = null
)
