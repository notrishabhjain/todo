package com.procrastinationkiller.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productivity_scores")
data class ProductivityScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val score: Int,
    val weekStart: Long,
    val completionRate: Float,
    val velocityScore: Float,
    val responseScore: Float,
    val backlogScore: Float
)
