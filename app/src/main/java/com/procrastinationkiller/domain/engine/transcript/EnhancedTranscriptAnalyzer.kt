package com.procrastinationkiller.domain.engine.transcript

import com.procrastinationkiller.domain.engine.KeywordEngine
import com.procrastinationkiller.domain.engine.TranscriptActionItem
import com.procrastinationkiller.domain.engine.TranscriptAnalyzer
import com.procrastinationkiller.domain.engine.ml.HybridClassificationPipeline
import com.procrastinationkiller.domain.model.TaskPriority

class EnhancedTranscriptAnalyzer(
    private val keywordEngine: KeywordEngine,
    private val pipeline: HybridClassificationPipeline?
) {

    private val speakerRoleDetector = SpeakerRoleDetector()

    fun analyze(transcript: String): List<TranscriptActionItem> {
        if (transcript.isBlank()) return emptyList()

        return try {
            val speakerProfiles = speakerRoleDetector.detectRoles(transcript)
            val contextBuilder = ConversationContextBuilder()
            val context = contextBuilder.build(transcript)

            val segments = splitIntoSegments(transcript)
            val sectionTopics = detectSectionTopics(transcript)
            val rawItems = mutableListOf<TranscriptActionItem>()
            var currentSectionTopic: String? = null

            for (segment in segments) {
                // Update section topic if the segment text matches a detected section header
                val sectionMatch = sectionTopics.find { it.second == segment.text }
                if (sectionMatch != null) {
                    currentSectionTopic = sectionMatch.first
                    continue
                }

                val keywordAnalysis = keywordEngine.analyze(segment.text)

                // If the segment contains Devanagari, also check Devanagari keywords
                val isDevanagariText = containsDevanagari(segment.text)

                val isActionable: Boolean
                val mlConfidence: Float?

                if (pipeline != null) {
                    val hybridResult = pipeline.classify(segment.text, keywordAnalysis)
                    isActionable = hybridResult.isActionable
                    mlConfidence = hybridResult.confidence
                } else {
                    // For Devanagari text, the keyword engine already checks Devanagari keywords
                    isActionable = keywordAnalysis.isActionable || (isDevanagariText && keywordAnalysis.actionKeywords.isNotEmpty())
                    mlConfidence = null
                }

                if (isActionable) {
                    val owner = detectOwner(segment)
                    val speakerRole = findSpeakerRole(segment.speaker, owner, speakerProfiles)
                    val priority = determinePriority(keywordAnalysis, context, speakerRole)
                    val confidence = calculateConfidence(keywordAnalysis, speakerRole, mlConfidence)
                    val topic = currentSectionTopic ?: context.currentTopic

                    rawItems.add(
                        TranscriptActionItem(
                            text = segment.text.trim(),
                            owner = owner,
                            priority = priority,
                            dueDate = keywordAnalysis.resolvedDueDate,
                            confidence = confidence,
                            speakerRole = speakerRole?.name,
                            meetingType = context.meetingType.name,
                            contextTopic = topic,
                            mlConfidence = mlConfidence
                        )
                    )
                }
            }

            deduplicateItems(rawItems)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Detects if text contains Devanagari characters (Unicode range U+0900 to U+097F).
     */
    fun containsDevanagari(text: String): Boolean {
        return text.any { it.code in 0x0900..0x097F }
    }

    /**
     * Detects section headers in the transcript.
     * A section header is a line that is all caps, or a short line (< 6 words) ending with ':'.
     */
    private fun detectSectionTopics(transcript: String): List<Pair<String, String>> {
        val sections = mutableListOf<Pair<String, String>>()
        val lines = transcript.split("\n").filter { it.isNotBlank() }

        for (rawLine in lines) {
            val line = rawLine.trim()
            val words = line.split("\\s+".toRegex())

            // All caps line (at least 2 chars, and all letters are uppercase)
            val letters = line.filter { it.isLetter() }
            if (letters.length >= 2 && letters.all { it.isUpperCase() }) {
                sections.add(Pair(line.lowercase().removeSuffix(":").trim(), line))
                continue
            }

            // Short line ending with ':' (< 6 words, not matching speaker pattern)
            if (line.endsWith(":") && words.size < 6) {
                val speakerMatch = SPEAKER_PATTERN.find(line)
                if (speakerMatch == null) {
                    sections.add(Pair(line.removeSuffix(":").trim().lowercase(), line))
                }
            }
        }

        return sections
    }

    private fun splitIntoSegments(transcript: String): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()
        val lines = transcript.split("\n").filter { it.isNotBlank() }
        var currentSpeaker: String? = null

        for (rawLine in lines) {
            val line = TranscriptAnalyzer.stripTimestamps(rawLine)
            val speakerMatch = SPEAKER_PATTERN.find(line)
            if (speakerMatch != null) {
                currentSpeaker = speakerMatch.groupValues[1].trim()
                val remainingText = line.substring(speakerMatch.range.last + 1).trim()
                if (remainingText.isNotEmpty()) {
                    val sentences = TranscriptAnalyzer.removeFillerWords(remainingText)
                        .split(SENTENCE_DELIMITER)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    for (sentence in sentences) {
                        segments.add(TranscriptSegment(sentence, currentSpeaker))
                    }
                }
            } else {
                val sentences = TranscriptAnalyzer.removeFillerWords(line)
                    .split(SENTENCE_DELIMITER)
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
        private val SPEAKER_PATTERN = TranscriptPatterns.SPEAKER_PATTERN
        private val SENTENCE_DELIMITER = Regex("[.!?;]+\\s*")
        private val AT_MENTION_PATTERN = Regex("@(\\w+)")
        private val ASSIGNMENT_PATTERN = Regex("^(\\w{2,}),?\\s+(?:please|will|should|needs? to|can you|could you)")
        private val HINDI_ASSIGNMENT_PATTERN = Regex("([\\w\\u0900-\\u097F]{2,})\\s+(?:ko|se|please|ko bol do|se karwao|ko bolo|se kaho|ko assign karo)\\s+")
    }
}
