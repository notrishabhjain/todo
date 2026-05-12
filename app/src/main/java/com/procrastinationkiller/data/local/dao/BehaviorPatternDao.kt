package com.procrastinationkiller.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.procrastinationkiller.data.local.entity.BehaviorPatternEntity

@Dao
interface BehaviorPatternDao {

    @Query("SELECT * FROM behavior_patterns WHERE sender = :sender")
    suspend fun getBySender(sender: String): List<BehaviorPatternEntity>

    @Query("SELECT * FROM behavior_patterns WHERE sourceApp = :sourceApp")
    suspend fun getBySourceApp(sourceApp: String): List<BehaviorPatternEntity>

    @Query("SELECT * FROM behavior_patterns")
    suspend fun getAll(): List<BehaviorPatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BehaviorPatternEntity): Long

    @Query("SELECT * FROM behavior_patterns ORDER BY ignoreCount DESC LIMIT :limit")
    suspend fun getTopIgnoredPatterns(limit: Int): List<BehaviorPatternEntity>

    @Query("SELECT * FROM behavior_patterns ORDER BY completionCount DESC LIMIT :limit")
    suspend fun getTopCompletedPatterns(limit: Int): List<BehaviorPatternEntity>
}
