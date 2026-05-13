package com.procrastinationkiller.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val achievementType: String,
    val unlockedAt: Long,
    val title: String,
    val description: String
)
