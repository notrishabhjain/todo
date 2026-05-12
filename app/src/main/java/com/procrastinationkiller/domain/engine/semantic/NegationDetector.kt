package com.procrastinationkiller.domain.engine.semantic

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NegationDetector @Inject constructor() {

    companion object {
        private val ENGLISH_NEGATION_PHRASES = listOf(
            "don't worry",
            "dont worry",
            "do not worry",
            "no need to",
            "no need",
            "nevermind",
            "never mind",
            "ignore that",
            "forget it",
            "forget about it",
            "not required",
            "not necessary",
            "don't bother",
            "dont bother",
            "skip it",
            "cancel that",
            "scratch that"
        )

        private val HINDI_HINGLISH_NEGATION_PHRASES = listOf(
            "nahi chahiye",
            "nahin chahiye",
            "rehne do",
            "rehne de",
            "mat karo",
            "mat kar",
            "zaroorat nahi",
            "zarurat nahi",
            "chhod do",
            "chod do",
            "jane do",
            "jaane do",
            "koi zarurat nahi",
            "nahi karna"
        )

        private val ACTION_VERBS = setOf(
            "send", "do", "complete", "finish", "submit", "review",
            "check", "call", "reply", "fix", "deploy", "write",
            "prepare", "update", "create", "forward", "buy", "pay"
        )
    }

    fun detect(text: String): NegationResult {
        val lowerText = text.lowercase().trim()

        // Check English negation phrases
        for (phrase in ENGLISH_NEGATION_PHRASES) {
            if (lowerText.contains(phrase)) {
                return NegationResult(
                    isNegated = true,
                    negationPhrase = phrase,
                    confidence = calculateConfidence(lowerText, phrase)
                )
            }
        }

        // Check Hindi/Hinglish negation phrases
        for (phrase in HINDI_HINGLISH_NEGATION_PHRASES) {
            if (lowerText.contains(phrase)) {
                return NegationResult(
                    isNegated = true,
                    negationPhrase = phrase,
                    confidence = calculateConfidence(lowerText, phrase)
                )
            }
        }

        // Check for negation before action verb pattern: "don't send", "do not send"
        if (hasNegationBeforeActionVerb(lowerText)) {
            val negPhrase = extractNegationPhrase(lowerText)
            return NegationResult(
                isNegated = true,
                negationPhrase = negPhrase,
                confidence = 0.8f
            )
        }

        return NegationResult(
            isNegated = false,
            negationPhrase = null,
            confidence = 0.0f
        )
    }

    private fun hasNegationBeforeActionVerb(text: String): Boolean {
        val words = text.split("\\s+".toRegex())
        for (i in words.indices) {
            if (words[i] in setOf("don't", "dont", "do not", "don\u2019t") ||
                (words[i] == "do" && i + 1 < words.size && words[i + 1] == "not")
            ) {
                // Check if there's an action verb after the negation
                val remaining = words.drop(i + 1).take(3)
                if (remaining.any { it in ACTION_VERBS }) {
                    return true
                }
            }
            if (words[i] == "not" && i + 1 < words.size && words[i + 1] in ACTION_VERBS) {
                return true
            }
        }
        return false
    }

    private fun extractNegationPhrase(text: String): String {
        val words = text.split("\\s+".toRegex())
        for (i in words.indices) {
            if (words[i] in setOf("don't", "dont", "don\u2019t")) {
                val end = minOf(i + 3, words.size)
                return words.subList(i, end).joinToString(" ")
            }
            if (words[i] == "do" && i + 1 < words.size && words[i + 1] == "not") {
                val end = minOf(i + 4, words.size)
                return words.subList(i, end).joinToString(" ")
            }
            if (words[i] == "not" && i + 1 < words.size && words[i + 1] in ACTION_VERBS) {
                val end = minOf(i + 3, words.size)
                return words.subList(i, end).joinToString(" ")
            }
        }
        return "negation detected"
    }

    private fun calculateConfidence(text: String, phrase: String): Float {
        // Higher confidence if the phrase is at the start of the message
        val isAtStart = text.trimStart().startsWith(phrase)
        // Higher confidence for longer/more explicit phrases
        val phraseWordCount = phrase.split("\\s+".toRegex()).size
        val baseConfidence = 0.75f
        val startBonus = if (isAtStart) 0.1f else 0.0f
        val lengthBonus = minOf(phraseWordCount * 0.05f, 0.15f)
        return minOf(baseConfidence + startBonus + lengthBonus, 1.0f)
    }
}
