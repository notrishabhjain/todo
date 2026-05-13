package com.procrastinationkiller.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.procrastinationkiller.data.local.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notifications ADD COLUMN notificationKey TEXT")
            db.execSQL("ALTER TABLE task_suggestions ADD COLUMN contentHash TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_notificationKey ON notifications(notificationKey)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_task_suggestions_contentHash ON task_suggestions(contentHash)")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_notificationKey ON notifications(notificationKey)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_task_suggestions_contentHash ON task_suggestions(contentHash)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "procrastination_killer_db"
        )
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao = database.notificationDao()

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideLearningDataDao(database: AppDatabase): LearningDataDao = database.learningDataDao()

    @Provides
    fun provideKeywordDao(database: AppDatabase): KeywordDao = database.keywordDao()

    @Provides
    fun provideAnalyticsDao(database: AppDatabase): AnalyticsDao = database.analyticsDao()

    @Provides
    fun provideTaskSuggestionDao(database: AppDatabase): TaskSuggestionDao = database.taskSuggestionDao()

    @Provides
    fun provideBehaviorPatternDao(database: AppDatabase): BehaviorPatternDao = database.behaviorPatternDao()

    @Provides
    fun provideProductivityScoreDao(database: AppDatabase): ProductivityScoreDao = database.productivityScoreDao()

    @Provides
    fun provideAchievementDao(database: AppDatabase): AchievementDao = database.achievementDao()
}
