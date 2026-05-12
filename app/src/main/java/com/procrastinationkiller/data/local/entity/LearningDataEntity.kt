package com.procrastinationkiller.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_data")
data class LearningDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val featureVector: String,
    val label: String,
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val feedbackType: String? = null,
    val sourceApp: String? = null,
    val sender: String? = null,
    val keywords: String? = null,
    val prioritySuggested: String? = null,
    val priorityFinal: String? = null
)
