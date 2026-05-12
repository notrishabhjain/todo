package com.procrastinationkiller.di

import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.domain.engine.learning.AdaptiveWeightManager
import com.procrastinationkiller.domain.engine.learning.LearningEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LearningModule {

    @Provides
    @Singleton
    fun provideAdaptiveWeightManager(
        learningDataDao: LearningDataDao
    ): AdaptiveWeightManager = AdaptiveWeightManager(learningDataDao)

    @Provides
    @Singleton
    fun provideLearningEngine(
        learningDataDao: LearningDataDao,
        adaptiveWeightManager: AdaptiveWeightManager
    ): LearningEngine = LearningEngine(learningDataDao, adaptiveWeightManager)
}
