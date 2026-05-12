package com.procrastinationkiller.di

import android.content.Context
import androidx.room.Room
import com.procrastinationkiller.data.local.AppDatabase
import com.procrastinationkiller.data.local.dao.AnalyticsDao
import com.procrastinationkiller.data.local.dao.ContactDao
import com.procrastinationkiller.data.local.dao.KeywordDao
import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.data.local.dao.NotificationDao
import com.procrastinationkiller.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "procrastination_killer_db"
        ).build()
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
}
