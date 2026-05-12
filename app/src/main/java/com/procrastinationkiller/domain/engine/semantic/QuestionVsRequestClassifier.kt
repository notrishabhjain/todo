package com.procrastinationkiller.domain.engine.semantic

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionVsRequestClassifier @Inject constructor() {

    companion object {
        private val QUESTION_STARTERS = setOf(
            "did", "have", "has", "is", "are", "was", "were",
            "do", "does", "would", "will", "shall", "should"
        )

        private val STATUS_CHECK_PATTERNS = listOf(
            "have you finished",
            "have you done",
            "have you completed",
            "did you send",
            "did you finish",
            "did you complete",
            "did you do",
            "is it done",
            "is it ready",
            "has it been",
            "kya tumne bheja",
            "kya ho gaya",
            "kya hua",
            "ho gaya kya"
        )

        private val POLITE_REQUEST_PATTERNS = listOf(
            "can you",
            "could you",
            "would you mind",
            "would you please",
            "will you",
            "shall we",
            "kya tum kar sakte",
            "kya aap kar sakte"
        )

        private val ACTION_VERBS = setOf(
            "send", "do", "complete", "finish", "submit", "review",
            "check", "call", "reply", "fix", "deploy", "write",
            "prepare", "update", "create", "forward", "buy", "pay",
            "make", "get", "bring", "take", "put", "give", "tell",
            "schedule", "book", "order", "deliver", "approve", "confirm"
        )

        private val HINDI_QUESTION_STARTERS = setOf(
            "kya", "kab", "kaise", "kyun", "kaun"
        )
    }

    fun classify(text: String): QuestionClassification {
        val lowerText = text.lowercase().trim()

        // Check for status check patterns first (not actionable)
        for (pattern in STATUS_CHECK_PATTERNS) {
            if (lowerText.contains(pattern)) {
                return QuestionClassification(
                    isQuestion = true,
                    isPoliteRequest = false,
                    confidence = 0.9f
                )
            }
        }

        // Check polite request patterns (actionable)
        for (pattern in POLITE_REQUEST_PATTERNS) {
            if (lowerText.contains(pattern)) {
                // Verify there's an action verb following it
                val afterPattern = lowerText.substringAfter(pattern).trim()
                val hasActionVerb = ACTION_VERBS.any { verb ->
                    afterPattern.split("\\s+".toRegex()).take(3).any { it == verb }
                }
                if (hasActionVerb) {
                    return QuestionClassification(
                        isQuestion = false,
                        isPoliteRequest = true,
                        confidence = 0.85f
                    )
                }
            }
        }

        // Check for question mark with question starters
        val endsWithQuestion = lowerText.endsWith("?")
        val words = lowerText.replace("?", "").trim().split("\\s+".toRegex())
        val firstWord = words.firstOrNull() ?: ""

        if (endsWithQuestion && firstWord in QUESTION_STARTERS) {
            return QuestionClassification(
                isQuestion = true,
                isPoliteRequest = false,
                confidence = 0.9f
            )
        }

        // Hindi question detection
        if (firstWord in HINDI_QUESTION_STARTERS && endsWithQuestion) {
            return QuestionClassification(
                isQuestion = true,
                isPoliteRequest = false,
                confidence = 0.85f
            )
        }

        // Check for imperative form (verb at start = request/actionable)
        if (firstWord in ACTION_VERBS) {
            return QuestionClassification(
                isQuestion = false,
                isPoliteRequest = false,
                confidence = 0.85f
            )
        }

        // "please" + action verb = request
        if (firstWord == "please" && words.size > 1 && words[1] in ACTION_VERBS) {
            return QuestionClassification(
                isQuestion = false,
                isPoliteRequest = true,
                confidence = 0.9f
            )
        }

        // Question mark alone is a weaker signal
        if (endsWithQuestion) {
            return QuestionClassification(
                isQuestion = true,
                isPoliteRequest = false,
                confidence = 0.6f
            )
        }

        // Default: not a question
        return QuestionClassification(
            isQuestion = false,
            isPoliteRequest = false,
            confidence = 0.5f
        )
    }
}
