package com.procrastinationkiller.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.procrastinationkiller.data.local.entity.AnalyticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM analytics ORDER BY timestamp DESC")
    fun getAllAnalytics(): Flow<List<AnalyticsEntity>>

    @Query("SELECT * FROM analytics WHERE eventType = :eventType")
    fun getAnalyticsByType(eventType: String): Flow<List<AnalyticsEntity>>

    @Query("SELECT * FROM analytics WHERE taskId = :taskId")
    fun getAnalyticsForTask(taskId: Long): Flow<List<AnalyticsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: AnalyticsEntity): Long

    @Query("DELETE FROM analytics WHERE timestamp < :before")
    suspend fun deleteOldAnalytics(before: Long)
}
