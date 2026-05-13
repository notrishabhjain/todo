package com.procrastinationkiller.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_suggestions",
    indices = [Index(value = ["contentHash"])]
)
data class TaskSuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val suggestedTitle: String,
    val description: String,
    val priority: String,
    val dueDate: Long? = null,
    val sourceApp: String,
    val sender: String,
    val originalText: String,
    val confidence: Float,
    val autoApprove: Boolean = false,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val contentHash: String? = null
)
