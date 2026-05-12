package com.procrastinationkiller.domain.engine.prioritization

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextualSignalAnalyzer @Inject constructor() {

    companion object {
        private const val CAPS_RATIO_THRESHOLD = 0.5f
        private const val MIN_CAPS_LENGTH = 4
        private const val EXCLAMATION_HIGH_THRESHOLD = 3
        private const val REPEATED_MESSAGE_THRESHOLD = 2
    }

    fun analyze(
        text: String,
        sender: String = "",
        recentMessagesFromSender: Int = 0
    ): ContextualSignalResult {
        val factors = mutableListOf<String>()
        var signalScore = 0f

        // Check ALL CAPS ratio
        val capsScore = calculateCapsScore(text)
        if (capsScore > 0f) {
            signalScore += capsScore
            factors.add("ALL_CAPS_DETECTED")
        }

        // Check exclamation marks
        val exclamationScore = calculateExclamationScore(text)
        if (exclamationScore > 0f) {
            signalScore += exclamationScore
            factors.add("EXCLAMATION_MARKS")
        }

        // Check repeated messages from same sender
        val repeatedScore = calculateRepeatedMessageScore(recentMessagesFromSender)
        if (repeatedScore > 0f) {
            signalScore += repeatedScore
            factors.add("REPEATED_MESSAGES")
        }

        // Normalize to 0-1 range
        val normalizedScore = signalScore.coerceIn(0f, 1f)

        return ContextualSignalResult(
            signalScore = normalizedScore,
            factors = factors
        )
    }

    private fun calculateCapsScore(text: String): Float {
        val letters = text.filter { it.isLetter() }
        if (letters.length < MIN_CAPS_LENGTH) return 0f

        val capsRatio = letters.count { it.isUpperCase() }.toFloat() / letters.length
        return if (capsRatio >= CAPS_RATIO_THRESHOLD) {
            (capsRatio * 0.4f).coerceAtMost(0.4f)
        } else {
            0f
        }
    }

    private fun calculateExclamationScore(text: String): Float {
        val exclamationCount = text.count { it == '!' }
        return when {
            exclamationCount >= EXCLAMATION_HIGH_THRESHOLD -> 0.3f
            exclamationCount >= 1 -> 0.1f * exclamationCount
            else -> 0f
        }
    }

    private fun calculateRepeatedMessageScore(recentMessagesFromSender: Int): Float {
        return if (recentMessagesFromSender >= REPEATED_MESSAGE_THRESHOLD) {
            (recentMessagesFromSender * 0.15f).coerceAtMost(0.3f)
        } else {
            0f
        }
    }
}

data class ContextualSignalResult(
    val signalScore: Float,
    val factors: List<String>
)
