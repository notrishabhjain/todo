package com.procrastinationkiller.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.procrastinationkiller.data.local.dao.AchievementDao
import com.procrastinationkiller.data.local.dao.AnalyticsDao
import com.procrastinationkiller.data.local.dao.BehaviorPatternDao
import com.procrastinationkiller.data.local.dao.ContactDao
import com.procrastinationkiller.data.local.dao.KeywordDao
import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.dao.NotificationDao
import com.procrastinationkiller.data.local.dao.ProductivityScoreDao
import com.procrastinationkiller.data.local.dao.TaskDao
import com.procrastinationkiller.data.local.dao.TaskSuggestionDao
import com.procrastinationkiller.data.local.entity.AchievementEntity
import com.procrastinationkiller.data.local.entity.AnalyticsEntity
import com.procrastinationkiller.data.local.entity.BehaviorPatternEntity
import com.procrastinationkiller.data.local.entity.ContactEntity
import com.procrastinationkiller.data.local.entity.KeywordEntity
import com.procrastinationkiller.data.local.entity.LearningDataEntity
import com.procrastinationkiller.data.local.entity.NotificationEntity
import com.procrastinationkiller.data.local.entity.ProductivityScoreEntity
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity

@Database(
    entities = [
        TaskEntity::class,
        NotificationEntity::class,
        ContactEntity::class,
        LearningDataEntity::class,
        KeywordEntity::class,
        AnalyticsEntity::class,
        TaskSuggestionEntity::class,
        BehaviorPatternEntity::class,
        ProductivityScoreEntity::class,
        AchievementEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao
    abstract fun contactDao(): ContactDao
    abstract fun learningDataDao(): LearningDataDao
    abstract fun keywordDao(): KeywordDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun taskSuggestionDao(): TaskSuggestionDao
    abstract fun behaviorPatternDao(): BehaviorPatternDao
    abstract fun productivityScoreDao(): ProductivityScoreDao
    abstract fun achievementDao(): AchievementDao
}
