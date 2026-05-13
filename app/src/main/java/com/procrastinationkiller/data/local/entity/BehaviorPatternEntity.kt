package com.procrastinationkiller.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behavior_patterns")
data class BehaviorPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val sourceApp: String,
    val taskType: String,
    val avgCompletionTimeMs: Long,
    val completionCount: Int,
    val ignoreCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)
