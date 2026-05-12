package com.procrastinationkiller.domain.engine

import com.procrastinationkiller.domain.engine.ml.HybridClassificationPipeline
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskSuggestion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskExtractionEngine @Inject constructor(
    private val keywordEngine: KeywordEngine,
    private val classificationPipeline: HybridClassificationPipeline? = null
) {

    fun extract(
        text: String,
        sourceApp: String = "",
        sender: String = ""
    ): TaskSuggestion? {
        val analysis = keywordEngine.analyze(text)

        // Use hybrid pipeline if available
        val hybridResult = classificationPipeline?.classify(text, analysis)

        val isActionable = hybridResult?.isActionable ?: analysis.isActionable

        if (!isActionable) {
            return null
        }

        val title = generateTitle(text, analysis)
        val priority = hybridResult?.finalPriority ?: determinePriority(analysis)
        val confidence = hybridResult?.confidence ?: calculateConfidence(analysis)

        return TaskSuggestion(
            suggestedTitle = title,
            description = text,
            priority = priority,
            dueDate = analysis.resolvedDueDate,
            sourceApp = sourceApp,
            sender = sender,
            originalText = text,
            confidence = confidence
        )
    }

    private fun generateTitle(text: String, analysis: KeywordAnalysis): String {
        val actionKeyword = analysis.actionKeywords.firstOrNull()?.keyword ?: ""

        val words = text.split("\\s+".toRegex())

        // Try to extract a concise action phrase from the text
        val actionIndex = if (actionKeyword.isNotEmpty()) {
            val lowerWords = text.lowercase().split("\\s+".toRegex())
            if (actionKeyword.contains(" ")) {
                val firstWord = actionKeyword.split(" ").first()
                lowerWords.indexOfFirst { it.contains(firstWord) }
            } else {
                lowerWords.indexOfFirst { it.contains(actionKeyword) }
            }
        } else {
            -1
        }

        val titleWords = if (actionIndex >= 0) {
            // Take a window around the action keyword to build title
            val start = maxOf(0, actionIndex - 1)
            val end = minOf(words.size, actionIndex + 5)
            words.subList(start, end)
        } else {
            words.take(6)
        }

        val title = titleWords.joinToString(" ")
            .trim()
            .replaceFirstChar { it.uppercase() }

        return if (title.length > 60) title.take(57) + "..." else title
    }

    private fun determinePriority(analysis: KeywordAnalysis): TaskPriority {
        val urgencyCount = analysis.urgencyKeywords.size

        return when {
            urgencyCount >= 2 -> TaskPriority.CRITICAL
            urgencyCount == 1 -> TaskPriority.HIGH
            analysis.timeIndicators.isNotEmpty() -> TaskPriority.HIGH
            else -> TaskPriority.MEDIUM
        }
    }

    private fun calculateConfidence(analysis: KeywordAnalysis): Float {
        var score = 0f

        // Base score for having action keywords
        score += minOf(analysis.actionKeywords.size * 0.3f, 0.5f)

        // Bonus for urgency
        score += minOf(analysis.urgencyKeywords.size * 0.15f, 0.25f)

        // Bonus for time indicators
        score += minOf(analysis.timeIndicators.size * 0.15f, 0.25f)

        return minOf(score, 1.0f)
    }
}
