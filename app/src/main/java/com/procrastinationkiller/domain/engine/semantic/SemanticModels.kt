package com.procrastinationkiller.domain.engine.semantic

import com.procrastinationkiller.domain.model.TaskPriority

data class SemanticAnalysisResult(
    val isActionable: Boolean,
    val detectedIntent: String?,
    val negationDetected: Boolean,
    val isQuestion: Boolean,
    val implicitDeadline: ImplicitDeadline?,
    val semanticConfidence: Float,
    val contextualFactors: Map<String, Float> = emptyMap()
)

data class NegationResult(
    val isNegated: Boolean,
    val negationPhrase: String?,
    val confidence: Float
)

data class QuestionClassification(
    val isQuestion: Boolean,
    val isPoliteRequest: Boolean,
    val confidence: Float
)

data class MessageContext(
    val text: String,
    val sender: String,
    val timestamp: Long,
    val actionVerbs: List<String> = emptyList()
)

data class ImplicitDeadline(
    val resolvedTimestamp: Long,
    val sourcePhrase: String
)

data class DeduplicationResult(
    val isDuplicate: Boolean,
    val existingTaskId: Long?,
    val similarityScore: Float
)

data class DisambiguationResult(
    val adjustedPriority: TaskPriority,
    val senderContext: Float
)
