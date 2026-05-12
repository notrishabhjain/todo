package com.procrastinationkiller.domain.engine

import com.procrastinationkiller.domain.engine.ml.HybridClassificationPipeline
import com.procrastinationkiller.domain.engine.transcript.EnhancedTranscriptAnalyzer
import com.procrastinationkiller.domain.model.TaskPriority
import javax.inject.Inject
import javax.inject.Singleton

data class TranscriptActionItem(
    val text: String,
    val owner: String?,
    val priority: TaskPriority,
    val dueDate: Long?,
    val confidence: Float,
    val speakerRole: String? = null,
    val meetingType: String? = null,
    val contextTopic: String? = null,
    val mlConfidence: Float? = null
)

@Singleton
class TranscriptAnalyzer @Inject constructor(
    private val keywordEngine: KeywordEngine
) {

    fun analyze(transcript: String): List<TranscriptActionItem> {
        val segments = splitIntoSegments(transcript)
        val actionItems = mutableListOf<TranscriptActionItem>()

        for (segment in segments) {
            val analysis = keywordEngine.analyze(segment.text)
            if (analysis.isActionable) {
                val owner = detectOwner(segment)
                val priority = determinePriority(analysis)
                actionItems.add(
                    TranscriptActionItem(
                        text = segment.text.trim(),
                        owner = owner,
                        priority = priority,
                        dueDate = analysis.resolvedDueDate,
                        confidence = calculateConfidence(analysis)
                    )
                )
            }
        }

        return actionItems
    }

    private fun splitIntoSegments(transcript: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        val lines = transcript.split("\n").filter { it.isNotBlank() }

        var currentSpeaker: String? = null

        for (line in lines) {
            val speakerMatch = SPEAKER_PATTERN.find(line)
            if (speakerMatch != null) {
                currentSpeaker = speakerMatch.groupValues[1].trim()
                val remainingText = line.substring(speakerMatch.range.last + 1).trim()
                if (remainingText.isNotEmpty()) {
                    // Split by sentences
                    val sentences = splitBySentences(remainingText)
                    for (sentence in sentences) {
                        segments.add(TranscriptSegment(sentence, currentSpeaker))
                    }
                }
            } else {
                // No speaker prefix, split by sentences
                val sentences = splitBySentences(line)
                for (sentence in sentences) {
                    segments.add(TranscriptSegment(sentence, currentSpeaker))
                }
            }
        }

        return segments
    }

    private fun splitBySentences(text: String): List<String> {
        return text.split(SENTENCE_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun detectOwner(segment: TranscriptSegment): String? {
        val text = segment.text

        // Pattern: @person will do X
        val atMentionMatch = AT_MENTION_PATTERN.find(text)
        if (atMentionMatch != null) {
            return atMentionMatch.groupValues[1]
        }

        // Pattern: Person, please do X / Person will do X
        val assignmentMatch = ASSIGNMENT_PATTERN.find(text)
        if (assignmentMatch != null) {
            return assignmentMatch.groupValues[1]
        }

        // Pattern: Person ko / Person se (Hindi assignment)
        val hindiAssignmentMatch = HINDI_ASSIGNMENT_PATTERN.find(text)
        if (hindiAssignmentMatch != null) {
            return hindiAssignmentMatch.groupValues[1]
        }

        // If there is a speaker context, might be self-assigned
        return segment.speaker
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
        score += minOf(analysis.actionKeywords.size * 0.3f, 0.5f)
        score += minOf(analysis.urgencyKeywords.size * 0.15f, 0.25f)
        score += minOf(analysis.timeIndicators.size * 0.15f, 0.25f)
        return minOf(score, 1.0f)
    }

    private data class TranscriptSegment(
        val text: String,
        val speaker: String?
    )

    companion object {
        private val SPEAKER_PATTERN = Regex("^([A-Za-z\\s]+):\\s*")
        private val SENTENCE_DELIMITER = Regex("[.!?;]+\\s*")
        private val AT_MENTION_PATTERN = Regex("@(\\w+)")
        private val ASSIGNMENT_PATTERN = Regex("^(\\w{2,}),?\\s+(?:please|will|should|needs? to|can you|could you)")
        private val HINDI_ASSIGNMENT_PATTERN = Regex("(\\w{2,})\\s+(?:ko|se|please)\\s+")
    }

    fun enhancedAnalyze(
        transcript: String,
        pipeline: HybridClassificationPipeline? = null
    ): List<TranscriptActionItem> {
        val enhancedAnalyzer = EnhancedTranscriptAnalyzer(keywordEngine, pipeline)
        return enhancedAnalyzer.analyze(transcript)
    }
}
