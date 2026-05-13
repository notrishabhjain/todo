package com.procrastinationkiller.domain.engine.prioritization

import com.procrastinationkiller.data.local.dao.BehaviorPatternDao
import com.procrastinationkiller.data.local.entity.BehaviorPatternEntity

class FakeBehaviorPatternDao : BehaviorPatternDao {
    val patterns = mutableListOf<BehaviorPatternEntity>()

    override suspend fun getBySender(sender: String): List<BehaviorPatternEntity> =
        patterns.filter { it.sender == sender }

    override suspend fun getBySourceApp(sourceApp: String): List<BehaviorPatternEntity> =
        patterns.filter { it.sourceApp == sourceApp }

    override suspend fun getAll(): List<BehaviorPatternEntity> = patterns.toList()

    override suspend fun upsert(entity: BehaviorPatternEntity): Long {
        patterns.add(entity)
        return patterns.size.toLong()
    }

    override suspend fun getTopIgnoredPatterns(limit: Int): List<BehaviorPatternEntity> =
        patterns.sortedByDescending { it.ignoreCount }.take(limit)

    override suspend fun getTopCompletedPatterns(limit: Int): List<BehaviorPatternEntity> =
        patterns.sortedByDescending { it.completionCount }.take(limit)
}
