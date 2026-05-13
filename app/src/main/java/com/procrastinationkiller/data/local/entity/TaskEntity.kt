package com.procrastinationkiller.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM",
    val status: String = "PENDING",
    val reminderMode: String = "NORMAL",
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val sourceApp: String? = null,
    val sender: String? = null,
    val originalText: String? = null,
    val tags: String? = null,
    val notes: String? = null
)
