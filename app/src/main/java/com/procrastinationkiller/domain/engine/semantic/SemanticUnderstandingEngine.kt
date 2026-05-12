package com.procrastinationkiller.domain.engine.semantic

import com.procrastinationkiller.domain.model.ContactPriority
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemanticUnderstandingEngine @Inject constructor(
    private val negationDetector: NegationDetector,
    private val questionClassifier: QuestionVsRequestClassifier,
    private val conversationFlowAnalyzer: ConversationFlowAnalyzer,
    private val implicitDeadlineResolver: ImplicitDeadlineResolver,
    private val contextualDisambiguator: ContextualDisambiguator
) {

    fun analyze(
        text: String,
        sender: String,
        sourceApp: String,
        contactPriority: ContactPriority? = null
    ): SemanticAnalysisResult {
        // Step 1: Check negation
        val negationResult = negationDetector.detect(text)
        if (negationResult.isNegated && negationResult.confidence > 0.7f) {
            return SemanticAnalysisResult(
                isActionable = false,
                detectedIntent = "NEGATION",
                negationDetected = true,
                isQuestion = false,
                implicitDeadline = null,
                semanticConfidence = negationResult.confidence,
                contextualFactors = mapOf("negation_confidence" to negationResult.confidence)
            )
        }

        // Step 2: Classify question vs request
        val questionResult = questionClassifier.classify(text)
        if (questionResult.isQuestion && !questionResult.isPoliteRequest) {
            return SemanticAnalysisResult(
                isActionable = false,
                detectedIntent = "QUESTION",
                negationDetected = false,
                isQuestion = true,
                implicitDeadline = null,
                semanticConfidence = questionResult.confidence,
                contextualFactors = mapOf("question_confidence" to questionResult.confidence)
            )
        }

        // Step 3: Analyze conversation flow for context
        conversationFlowAnalyzer.addMessage(sender, text)
        val resolvedContext = conversationFlowAnalyzer.resolveContext(sender, text)

        // Step 4: Resolve implicit deadlines
        val textToResolve = if (resolvedContext != null) {
            "$text ${resolvedContext.text}"
        } else {
            text
        }
        val implicitDeadline = implicitDeadlineResolver.resolve(textToResolve)

        // Step 5: Contextual disambiguation
        val disambiguation = contextualDisambiguator.disambiguate(text, sender, contactPriority)

        // Compute semantic confidence
        val baseConfidence = if (questionResult.isPoliteRequest) 0.8f else 0.7f
        val contextBoost = if (resolvedContext != null) 0.05f else 0.0f
        val deadlineBoost = if (implicitDeadline != null) 0.05f else 0.0f
        val semanticConfidence = minOf(baseConfidence + contextBoost + deadlineBoost, 1.0f)

        val contextualFactors = mutableMapOf(
            "sender_importance" to disambiguation.senderContext,
            "is_polite_request" to if (questionResult.isPoliteRequest) 1.0f else 0.0f
        )
        if (resolvedContext != null) {
            contextualFactors["has_context_resolution"] = 1.0f
        }

        return SemanticAnalysisResult(
            isActionable = true,
            detectedIntent = if (questionResult.isPoliteRequest) "POLITE_REQUEST" else "TASK_REQUEST",
            negationDetected = negationResult.isNegated,
            isQuestion = questionResult.isQuestion,
            implicitDeadline = implicitDeadline,
            semanticConfidence = semanticConfidence,
            contextualFactors = contextualFactors
        )
    }
}
