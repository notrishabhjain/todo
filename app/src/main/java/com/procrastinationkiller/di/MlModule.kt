package com.procrastinationkiller.di

import com.procrastinationkiller.domain.engine.ml.HybridClassificationPipeline
import com.procrastinationkiller.domain.engine.ml.OnnxIntentClassifier
import com.procrastinationkiller.domain.engine.ml.RuleBasedIntentClassifier
import com.procrastinationkiller.domain.engine.ml.TextFeatureExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MlModule {

    @Provides
    @Singleton
    fun provideTextFeatureExtractor(): TextFeatureExtractor = TextFeatureExtractor()

    @Provides
    @Singleton
    fun provideOnnxIntentClassifier(): OnnxIntentClassifier = OnnxIntentClassifier()

    @Provides
    @Singleton
    fun provideRuleBasedIntentClassifier(): RuleBasedIntentClassifier = RuleBasedIntentClassifier()

    @Provides
    @Singleton
    fun provideHybridClassificationPipeline(
        textFeatureExtractor: TextFeatureExtractor,
        onnxIntentClassifier: OnnxIntentClassifier,
        ruleBasedIntentClassifier: RuleBasedIntentClassifier
    ): HybridClassificationPipeline = HybridClassificationPipeline(
        textFeatureExtractor,
        onnxIntentClassifier,
        ruleBasedIntentClassifier
    )
}
