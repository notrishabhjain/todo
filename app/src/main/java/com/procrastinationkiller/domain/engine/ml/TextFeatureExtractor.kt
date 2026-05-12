package com.procrastinationkiller.domain.engine.ml

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextFeatureExtractor @Inject constructor() {

    companion object {
        const val FEATURE_COUNT = 8

        private val ACTION_VERBS = setOf(
            "do", "complete", "send", "submit", "call", "reply", "review",
            "check", "update", "schedule", "prepare", "fix", "deploy", "test",
            "follow", "remind", "finish", "deliver", "write", "create",
            "forward", "approve", "confirm", "book", "pay", "buy", "order",
            "karna", "bhejna", "dekho", "bhejo", "likho", "batana", "karo"
        )

        private val URGENCY_KEYWORDS = setOf(
            "urgent", "asap", "immediately", "high priority", "critical",
            "important", "deadline", "jaldi", "turant", "abhi", "zaruri", "fatafat"
        )

        private val TIME_REFERENCES = setOf(
            "tomorrow", "tonight", "today", "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday", "sunday", "next week", "eod",
            "end of day", "by evening", "by morning",
            "kal", "shaam", "subah", "aaj", "parso", "hafte"
        )
    }

    fun extract(text: String): FloatArray {
        val features = FloatArray(FEATURE_COUNT)
        val lowerText = text.lowercase()
        val words = lowerText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val wordCount = words.size

        // Feature 0: Word count (normalized, cap at 50)
        features[0] = (wordCount.toFloat() / 50f).coerceAtMost(1.0f)

        // Feature 1: Action verb density
        features[1] = if (wordCount > 0) {
            val actionCount = words.count { word -> ACTION_VERBS.any { word.contains(it) } }
            (actionCount.toFloat() / wordCount.toFloat()).coerceAtMost(1.0f)
        } else 0f

        // Feature 2: Urgency keyword density
        features[2] = if (wordCount > 0) {
            val urgencyCount = URGENCY_KEYWORDS.count { lowerText.contains(it) }
            (urgencyCount.toFloat() / wordCount.toFloat()).coerceAtMost(1.0f)
        } else 0f

        // Feature 3: Question mark presence (1.0 if present, 0.0 otherwise)
        features[3] = if (text.contains("?")) 1.0f else 0.0f

        // Feature 4: Exclamation count (normalized, cap at 5)
        features[4] = (text.count { it == '!' }.toFloat() / 5f).coerceAtMost(1.0f)

        // Feature 5: Time reference count (normalized, cap at 3)
        features[5] = (TIME_REFERENCES.count { lowerText.contains(it) }.toFloat() / 3f).coerceAtMost(1.0f)

        // Feature 6: Average word length (normalized, cap at 10)
        features[6] = if (wordCount > 0) {
            val avgLen = words.sumOf { it.length }.toFloat() / wordCount.toFloat()
            (avgLen / 10f).coerceAtMost(1.0f)
        } else 0f

        // Feature 7: Capitalization ratio
        features[7] = if (text.isNotEmpty()) {
            val letterCount = text.count { it.isLetter() }
            if (letterCount > 0) {
                text.count { it.isUpperCase() }.toFloat() / letterCount.toFloat()
            } else 0f
        } else 0f

        return features
    }
}
