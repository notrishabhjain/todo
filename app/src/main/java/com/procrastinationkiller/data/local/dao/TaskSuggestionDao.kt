package com.procrastinationkiller.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskSuggestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(suggestion: TaskSuggestionEntity): Long

    @Query("SELECT * FROM task_suggestions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TaskSuggestionEntity>>

    @Query("SELECT * FROM task_suggestions WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: String): Flow<List<TaskSuggestionEntity>>

    @Query("UPDATE task_suggestions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM task_suggestions WHERE id = :id")
    suspend fun delete(id: Long)
}
