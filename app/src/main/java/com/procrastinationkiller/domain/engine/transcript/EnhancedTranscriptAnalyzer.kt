package com.procrastinationkiller.domain.engine.transcript

import com.procrastinationkiller.domain.engine.KeywordEngine
import com.procrastinationkiller.domain.engine.TranscriptActionItem
import com.procrastinationkiller.domain.engine.ml.HybridClassificationPipeline
import com.procrastinationkiller.domain.model.TaskPriority

class EnhancedTranscriptAnalyzer(
    private val keywordEngine: KeywordEngine,
    private val pipeline: HybridClassificationPipeline?
) {

    private val speakerRoleDetector = SpeakerRoleDetector()

    fun analyze(transcript: String): List<TranscriptActionItem> {
        if (transcript.isBlank()) return emptyList()

        val speakerProfiles = speakerRoleDetector.detectRoles(transcript)
        val contextBuilder = ConversationContextBuilder()
        val context = contextBuilder.build(transcript)

        val segments = splitIntoSegments(transcript)
        val rawItems = mutableListOf<TranscriptActionItem>()

        for (segment in segments) {
            val keywordAnalysis = keywordEngine.analyze(segment.text)

            val isActionable: Boolean
            val mlConfidence: Float?

            if (pipeline != null) {
                val hybridResult = pipeline.classify(segment.text, keywordAnalysis)
                isActionable = hybridResult.isActionable
                mlConfidence = hybridResult.confidence
            } else {
                isActionable = keywordAnalysis.isActionable
                mlConfidence = null
            }

            if (isActionable) {
                val owner = detectOwner(segment)
                val speakerRole = findSpeakerRole(segment.speaker, owner, speakerProfiles)
                val priority = determinePriority(keywordAnalysis, context, speakerRole)
                val confidence = calculateConfidence(keywordAnalysis, speakerRole, mlConfidence)

                rawItems.add(
                    TranscriptActionItem(
                        text = segment.text.trim(),
                        owner = owner,
                        priority = priority,
                        dueDate = keywordAnalysis.resolvedDueDate,
                        confidence = confidence,
                        speakerRole = speakerRole?.name,
                        meetingType = context.meetingType.name,
                        contextTopic = context.currentTopic,
                        mlConfidence = mlConfidence
                    )
                )
            }
        }

        return deduplicateItems(rawItems)
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
                    val sentences = remainingText.split(SENTENCE_DELIMITER)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    for (sentence in sentences) {
                        segments.add(TranscriptSegment(sentence, currentSpeaker))
                    }
                }
            } else {
                val sentences = line.split(SENTENCE_DELIMITER)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                for (sentence in sentences) {
                    segments.add(TranscriptSegment(sentence, currentSpeaker))
                }
            }
        }

        return segments
    }

    private fun detectOwner(segment: TranscriptSegment): String? {
        val text = segment.text

        val atMentionMatch = AT_MENTION_PATTERN.find(text)
        if (atMentionMatch != null) {
            return atMentionMatch.groupValues[1]
        }

        val assignmentMatch = ASSIGNMENT_PATTERN.find(text)
        if (assignmentMatch != null) {
            return assignmentMatch.groupValues[1]
        }

        val hindiAssignmentMatch = HINDI_ASSIGNMENT_PATTERN.find(text)
        if (hindiAssignmentMatch != null) {
            return hindiAssignmentMatch.groupValues[1]
        }

        return segment.speaker
    }

    private fun findSpeakerRole(
        speaker: String?,
        owner: String?,
        profiles: List<SpeakerProfile>
    ): SpeakerRole? {
        // Look up the owner's role first, then the speaker's role
        val targetName = owner ?: speaker ?: return null
        val profile = profiles.find { it.name.equals(targetName, ignoreCase = true) }
        return profile?.role
    }

    private fun determinePriority(
        analysis: com.procrastinationkiller.domain.engine.KeywordAnalysis,
        context: ConversationContext,
        speakerRole: SpeakerRole?
    ): TaskPriority {
        val urgencyCount = analysis.urgencyKeywords.size

        // Context-aware priority adjustments
        val basePriority = when {
            urgencyCount >= 2 -> TaskPriority.CRITICAL
            urgencyCount == 1 -> TaskPriority.HIGH
            analysis.timeIndicators.isNotEmpty() -> TaskPriority.HIGH
            else -> TaskPriority.MEDIUM
        }

        // Standup blockers are always high priority
        if (context.meetingType == MeetingType.STANDUP && basePriority == TaskPriority.MEDIUM) {
            return TaskPriority.HIGH
        }

        // Tasks assigned by organizer to specific person get a boost
        if (speakerRole == SpeakerRole.ORGANIZER && basePriority == TaskPriority.MEDIUM) {
            return TaskPriority.HIGH
        }

        return basePriority
    }

    private fun calculateConfidence(
        analysis: com.procrastinationkiller.domain.engine.KeywordAnalysis,
        speakerRole: SpeakerRole?,
        mlConfidence: Float?
    ): Float {
        var score = 0f
        score += minOf(analysis.actionKeywords.size * 0.3f, 0.5f)
        score += minOf(analysis.urgencyKeywords.size * 0.15f, 0.25f)
        score += minOf(analysis.timeIndicators.size * 0.15f, 0.25f)

        // Context boosts confidence
        if (speakerRole == SpeakerRole.ORGANIZER) {
            score += 0.1f
        }

        // ML confidence blending
        if (mlConfidence != null) {
            score = (score + mlConfidence) / 2f
        }

        return minOf(score, 1.0f)
    }

    private fun deduplicateItems(items: List<TranscriptActionItem>): List<TranscriptActionItem> {
        if (items.size <= 1) return items

        val deduplicated = mutableListOf<TranscriptActionItem>()

        for (item in items) {
            val isDuplicate = deduplicated.any { existing ->
                isSimilar(existing.text, item.text)
            }
            if (!isDuplicate) {
                deduplicated.add(item)
            } else {
                // Merge: keep the one with higher confidence
                val existingIdx = deduplicated.indexOfFirst { isSimilar(it.text, item.text) }
                if (existingIdx >= 0 && item.confidence > deduplicated[existingIdx].confidence) {
                    deduplicated[existingIdx] = item
                }
            }
        }

        return deduplicated
    }

    private fun isSimilar(text1: String, text2: String): Boolean {
        val words1 = text1.lowercase().split("\\s+".toRegex()).toSet()
        val words2 = text2.lowercase().split("\\s+".toRegex()).toSet()

        if (words1.isEmpty() || words2.isEmpty()) return false

        val intersection = words1.intersect(words2)
        val union = words1.union(words2)

        val similarity = intersection.size.toFloat() / union.size.toFloat()
        return similarity > 0.7f
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
}
