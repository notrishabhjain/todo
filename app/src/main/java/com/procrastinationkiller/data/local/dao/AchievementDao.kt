package com.procrastinationkiller.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.procrastinationkiller.data.local.entity.AchievementEntity

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    suspend fun getAll(): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE achievementType = :type LIMIT 1")
    suspend fun getByType(type: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity): Long

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getUnlockedCount(): Int
}
