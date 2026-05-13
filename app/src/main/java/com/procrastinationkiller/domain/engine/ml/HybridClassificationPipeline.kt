package com.procrastinationkiller.domain.engine.ml

import android.util.Log
import com.procrastinationkiller.domain.engine.KeywordAnalysis
import com.procrastinationkiller.domain.engine.KeywordEngine
import com.procrastinationkiller.domain.model.TaskPriority
import javax.inject.Inject
import javax.inject.Singleton

enum class ClassificationSource {
    ML,
    RULES,
    HYBRID
}

data class HybridResult(
    val isActionable: Boolean,
    val finalPriority: TaskPriority?,
    val confidence: Float,
    val intent: IntentType,
    val source: ClassificationSource
)

@Singleton
class HybridClassificationPipeline @Inject constructor(
    private val textFeatureExtractor: TextFeatureExtractor,
    private val onnxIntentClassifier: OnnxIntentClassifier,
    private val ruleBasedIntentClassifier: RuleBasedIntentClassifier
) {

    fun classify(text: String, keywordAnalysis: KeywordAnalysis): HybridResult {
        // Step 1: Extract features from text
        val features = textFeatureExtractor.extract(text)

        // Step 2: Try ONNX classifier first
        val mlResult = onnxIntentClassifier.classify(features)

        // Step 3: Get rule-based classification as fallback/comparison
        val ruleResult = ruleBasedIntentClassifier.classify(features)

        // Step 4: Combine results
        val result = when {
            // ML available and confident (> 0.7): prefer ML
            mlResult != null && mlResult.confidence > 0.7f -> {
                HybridResult(
                    isActionable = mlResult.isActionable,
                    finalPriority = mlResult.suggestedPriority,
                    confidence = mlResult.confidence,
                    intent = mlResult.intent,
                    source = ClassificationSource.ML
                )
            }

            // ML available but low confidence: use hybrid approach
            mlResult != null -> {
                val isActionable = ruleResult.isActionable || keywordAnalysis.isActionable
                val priority = ruleResult.suggestedPriority ?: mlResult.suggestedPriority
                val confidence = (mlResult.confidence + ruleResult.confidence) / 2f
                HybridResult(
                    isActionable = isActionable,
                    finalPriority = priority,
                    confidence = confidence,
                    intent = ruleResult.intent,
                    source = ClassificationSource.HYBRID
                )
            }

            // ML unavailable: use rule-based only
            else -> {
                // Combine rule-based classification with keyword analysis
                val isActionable = ruleResult.isActionable || keywordAnalysis.isActionable
                HybridResult(
                    isActionable = isActionable,
                    finalPriority = ruleResult.suggestedPriority,
                    confidence = ruleResult.confidence,
                    intent = ruleResult.intent,
                    source = ClassificationSource.RULES
                )
            }
        }

        Log.d("HybridPipeline", "Classification: source=${result.source}, isActionable=${result.isActionable}, intent=${result.intent}, confidence=${result.confidence}")
        return result
    }
}
