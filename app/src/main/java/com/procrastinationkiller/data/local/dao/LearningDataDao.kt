package com.procrastinationkiller.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningDataDao {
    @Query("SELECT * FROM learning_data ORDER BY timestamp DESC")
    fun getAllLearningData(): Flow<List<LearningDataEntity>>

    @Query("SELECT * FROM learning_data WHERE label = :label")
    fun getLearningDataByLabel(label: String): Flow<List<LearningDataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningData(data: LearningDataEntity): Long

    @Query("DELETE FROM learning_data WHERE timestamp < :before")
    suspend fun deleteOldData(before: Long)

    @Query("SELECT * FROM learning_data WHERE sourceApp = :app ORDER BY timestamp DESC")
    suspend fun getBySourceApp(app: String): List<LearningDataEntity>

    @Query("SELECT * FROM learning_data WHERE sender = :sender ORDER BY timestamp DESC")
    suspend fun getBySender(sender: String): List<LearningDataEntity>

    @Query("SELECT * FROM learning_data WHERE feedbackType = :type ORDER BY timestamp DESC")
    suspend fun getByFeedbackType(type: String): List<LearningDataEntity>

    @Query("SELECT * FROM learning_data ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentDataList(limit: Int): List<LearningDataEntity>

    @Query("SELECT COUNT(*) FROM learning_data WHERE label = :label")
    suspend fun getCountByLabel(label: String): Int

    @Query("DELETE FROM learning_data WHERE id NOT IN (SELECT id FROM learning_data ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun deleteOldest(keepCount: Int)
}
