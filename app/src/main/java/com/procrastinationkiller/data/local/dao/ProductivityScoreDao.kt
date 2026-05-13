package com.procrastinationkiller.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.procrastinationkiller.data.local.entity.ProductivityScoreEntity

@Dao
interface ProductivityScoreDao {
    @Query("SELECT * FROM productivity_scores ORDER BY weekStart DESC LIMIT 1")
    suspend fun getLatestScore(): ProductivityScoreEntity?

    @Query("SELECT * FROM productivity_scores WHERE weekStart BETWEEN :startTime AND :endTime ORDER BY weekStart ASC")
    suspend fun getScoresInRange(startTime: Long, endTime: Long): List<ProductivityScoreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ProductivityScoreEntity): Long

    @Query("SELECT * FROM productivity_scores ORDER BY weekStart DESC LIMIT :limit")
    suspend fun getWeeklyTrend(limit: Int): List<ProductivityScoreEntity>
}
