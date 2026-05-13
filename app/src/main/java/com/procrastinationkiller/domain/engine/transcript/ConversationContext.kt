package com.procrastinationkiller.domain.engine.transcript

enum class MeetingType {
    STANDUP,
    PLANNING,
    REVIEW,
    GENERAL
}

data class ConversationContext(
    val currentTopic: String?,
    val participants: List<String>,
    val meetingType: MeetingType
)

class ConversationContextBuilder {

    private val participants = mutableListOf<String>()
    private var detectedMeetingType: MeetingType = MeetingType.GENERAL
    private var currentTopic: String? = null

    fun build(transcript: String): ConversationContext {
        val lines = transcript.split("\n").filter { it.isNotBlank() }

        for (line in lines) {
            val speakerMatch = SPEAKER_PATTERN.find(line)
            if (speakerMatch != null) {
                val speaker = speakerMatch.groupValues[1].trim()
                if (speaker !in participants) {
                    participants.add(speaker)
                }
            }
        }

        detectedMeetingType = detectMeetingType(transcript)
        currentTopic = extractTopic(transcript)

        return ConversationContext(
            currentTopic = currentTopic,
            participants = participants.toList(),
            meetingType = detectedMeetingType
        )
    }

    private fun detectMeetingType(transcript: String): MeetingType {
        val lowerText = transcript.lowercase()

        val standupScore = STANDUP_KEYWORDS.count { lowerText.contains(it) }
        val planningScore = PLANNING_KEYWORDS.count { lowerText.contains(it) }
        val reviewScore = REVIEW_KEYWORDS.count { lowerText.contains(it) }

        val maxScore = maxOf(standupScore, planningScore, reviewScore)
        if (maxScore == 0) return MeetingType.GENERAL

        return when (maxScore) {
            standupScore -> MeetingType.STANDUP
            planningScore -> MeetingType.PLANNING
            reviewScore -> MeetingType.REVIEW
            else -> MeetingType.GENERAL
        }
    }

    private fun extractTopic(transcript: String): String? {
        val lowerText = transcript.lowercase()

        // Try to extract topic from common patterns
        val topicMatch = TOPIC_PATTERN.find(lowerText)
        if (topicMatch != null) {
            return topicMatch.groupValues[1].trim()
        }

        // Fallback: use meeting type as topic indicator
        return when (detectedMeetingType) {
            MeetingType.STANDUP -> "daily standup"
            MeetingType.PLANNING -> "sprint planning"
            MeetingType.REVIEW -> "review"
            MeetingType.GENERAL -> null
        }
    }

    companion object {
        private val SPEAKER_PATTERN = Regex("^(?:\\[?\\d{1,2}[:.\\-]\\d{2}(?:[:.\\-]\\d{2})?\\]?\\s*[-]?\\s*)?\\*{0,2}([A-Za-z][A-Za-z0-9\\s]*?)\\*{0,2}\\s*:\\s*")
        private val TOPIC_PATTERN = Regex("(?:discuss|talking about|topic is|agenda:?)\\s+(.+?)(?:\\.|\\n|$)")

        private val STANDUP_KEYWORDS = listOf(
            "blocker", "blockers", "yesterday", "today", "standup",
            "stand-up", "what did you", "stuck on", "impediment"
        )

        private val PLANNING_KEYWORDS = listOf(
            "sprint", "planning", "story points", "backlog", "estimate",
            "velocity", "capacity", "user story", "epic", "iteration"
        )

        private val REVIEW_KEYWORDS = listOf(
            "review", "demo", "feedback", "pr review", "code review",
            "pull request", "looks good", "approve", "changes requested"
        )
    }
}
