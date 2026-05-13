package com.procrastinationkiller.domain.engine.ml

import android.util.Log
import com.procrastinationkiller.domain.model.TaskPriority
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback intent classifier that uses feature vector thresholds
 * to classify intent without an ML model.
 * This ensures the app works immediately after install before any model download.
 */
@Singleton
class RuleBasedIntentClassifier @Inject constructor() : IntentClassifier {

    override fun classify(features: FloatArray): ClassificationResult {
        require(features.size >= TextFeatureExtractor.FEATURE_COUNT) {
            "Feature array must have at least ${TextFeatureExtractor.FEATURE_COUNT} elements"
        }

        val wordCountNorm = features[0]
        val actionVerbDensity = features[1]
        val urgencyDensity = features[2]
        val hasQuestion = features[3] > 0.5f
        val exclamationNorm = features[4]
        val timeRefNorm = features[5]
        val avgWordLenNorm = features[6]
        val capsRatio = features[7]

        Log.d("RuleBasedClassifier", "Classifying: actionVerbDensity=$actionVerbDensity, urgencyDensity=$urgencyDensity, timeRef=$timeRefNorm")

        // Classify based on feature thresholds
        val result = when {
            // Meeting: time references present with action verbs
            timeRefNorm > 0.3f && actionVerbDensity > 0.05f && hasMeetingSignal(actionVerbDensity, timeRefNorm) -> {
                val confidence = ((timeRefNorm + actionVerbDensity) / 2f).coerceAtMost(0.9f)
                ClassificationResult(
                    intent = IntentType.MEETING,
                    confidence = confidence,
                    isActionable = true,
                    suggestedPriority = TaskPriority.HIGH
                )
            }

            // Question: has question mark with low action verb density
            hasQuestion && actionVerbDensity < 0.15f -> {
                val confidence = 0.6f + (if (urgencyDensity > 0f) 0.1f else 0f)
                ClassificationResult(
                    intent = IntentType.QUESTION,
                    confidence = confidence.coerceAtMost(0.85f),
                    isActionable = false,
                    suggestedPriority = null
                )
            }

            // Task request: action verb density > 0.15 or urgency present
            actionVerbDensity > 0.15f || (actionVerbDensity > 0.05f && urgencyDensity > 0f) -> {
                val baseConfidence = 0.5f + (actionVerbDensity * 0.3f) + (urgencyDensity * 0.2f)
                val priority = when {
                    urgencyDensity > 0.1f -> TaskPriority.CRITICAL
                    urgencyDensity > 0f || timeRefNorm > 0.3f -> TaskPriority.HIGH
                    else -> TaskPriority.MEDIUM
                }
                ClassificationResult(
                    intent = IntentType.TASK_REQUEST,
                    confidence = baseConfidence.coerceAtMost(0.95f),
                    isActionable = true,
                    suggestedPriority = priority
                )
            }

            // Reminder: has time references but low action density
            timeRefNorm > 0f && actionVerbDensity <= 0.15f -> {
                ClassificationResult(
                    intent = IntentType.REMINDER,
                    confidence = 0.5f + timeRefNorm * 0.2f,
                    isActionable = true,
                    suggestedPriority = TaskPriority.MEDIUM
                )
            }

            // Social: exclamation or caps with no action content
            exclamationNorm > 0.2f && actionVerbDensity == 0f -> {
                ClassificationResult(
                    intent = IntentType.SOCIAL,
                    confidence = 0.6f,
                    isActionable = false,
                    suggestedPriority = null
                )
            }

            // Default: Information
            else -> {
                ClassificationResult(
                    intent = IntentType.INFORMATION,
                    confidence = 0.5f,
                    isActionable = false,
                    suggestedPriority = null
                )
            }
        }

        Log.d("RuleBasedClassifier", "Result: intent=${result.intent}, isActionable=${result.isActionable}, confidence=${result.confidence}")
        return result
    }

    private fun hasMeetingSignal(actionVerbDensity: Float, timeRefNorm: Float): Boolean {
        // Meeting signals are strong when both time and action are present at moderate levels
        return timeRefNorm > 0.5f && actionVerbDensity > 0.1f
    }
}
