package com.procrastinationkiller.di

import android.util.Log
import com.procrastinationkiller.data.local.dao.LearningDataDao
import com.procrastinationkiller.domain.engine.learning.AdaptiveWeightManager
import com.procrastinationkiller.domain.engine.learning.LearningEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LearningModule {

    @Provides
    @Singleton
    fun provideAdaptiveWeightManager(
        learningDataDao: LearningDataDao
    ): AdaptiveWeightManager {
        val manager = AdaptiveWeightManager(learningDataDao)
        // Load persisted weights on creation so learned state survives process restart.
        // Uses SupervisorJob so failure does not propagate, and logs errors for diagnostics.
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("LearningModule", "Failed to load adaptive weights from database", throwable)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler).launch {
            manager.loadFromDatabase()
        }
        return manager
    }

    @Provides
    @Singleton
    fun provideLearningEngine(
        learningDataDao: LearningDataDao,
        adaptiveWeightManager: AdaptiveWeightManager
    ): LearningEngine = LearningEngine(learningDataDao, adaptiveWeightManager)
}
