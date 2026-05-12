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
}
