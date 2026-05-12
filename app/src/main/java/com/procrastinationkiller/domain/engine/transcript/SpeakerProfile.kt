package com.procrastinationkiller.domain.engine.transcript

enum class SpeakerRole {
    ORGANIZER,
    PARTICIPANT,
    UNKNOWN
}

data class SpeakerProfile(
    val name: String,
    val role: SpeakerRole,
    val assignmentCount: Int = 0,
    val mentionCount: Int = 0
)

class SpeakerRoleDetector {

    fun detectRoles(transcript: String): List<SpeakerProfile> {
        val lines = transcript.split("\n").filter { it.isNotBlank() }
        val speakerOrder = mutableListOf<String>()
        val assignmentCounts = mutableMapOf<String, Int>()
        val mentionCounts = mutableMapOf<String, Int>()

        for (line in lines) {
            val speakerMatch = SPEAKER_PATTERN.find(line)
            if (speakerMatch != null) {
                val speaker = speakerMatch.groupValues[1].trim()
                if (speaker !in speakerOrder) {
                    speakerOrder.add(speaker)
                }
                val remainingText = line.substring(speakerMatch.range.last + 1).trim()
                detectAssignments(remainingText, assignmentCounts, mentionCounts)
            } else {
                detectAssignments(line.trim(), assignmentCounts, mentionCounts)
            }
        }

        return speakerOrder.map { speaker ->
            val role = inferRole(speaker, speakerOrder, assignmentCounts)
            SpeakerProfile(
                name = speaker,
                role = role,
                assignmentCount = assignmentCounts[speaker] ?: 0,
                mentionCount = mentionCounts[speaker] ?: 0
            )
        }
    }

    private fun detectAssignments(
        text: String,
        assignmentCounts: MutableMap<String, Int>,
        mentionCounts: MutableMap<String, Int>
    ) {
        val lowerText = text.lowercase()

        // Detect patterns like "Person, please..." or "@Person"
        val assignmentMatch = ASSIGNMENT_PATTERN.find(text)
        if (assignmentMatch != null) {
            val assignee = assignmentMatch.groupValues[1]
            assignmentCounts[assignee] = (assignmentCounts[assignee] ?: 0) + 1
            mentionCounts[assignee] = (mentionCounts[assignee] ?: 0) + 1
        }

        val atMentionMatch = AT_MENTION_PATTERN.find(text)
        if (atMentionMatch != null) {
            val mentioned = atMentionMatch.groupValues[1]
            mentionCounts[mentioned] = (mentionCounts[mentioned] ?: 0) + 1
            if (ASSIGNMENT_KEYWORDS.any { lowerText.contains(it) }) {
                assignmentCounts[mentioned] = (assignmentCounts[mentioned] ?: 0) + 1
            }
        }
    }

    private fun inferRole(
        speaker: String,
        speakerOrder: List<String>,
        assignmentCounts: Map<String, Int>
    ): SpeakerRole {
        // First speaker is likely the organizer
        if (speakerOrder.isNotEmpty() && speakerOrder[0] == speaker) {
            return SpeakerRole.ORGANIZER
        }

        // Person frequently assigned tasks is a participant
        val assignments = assignmentCounts[speaker] ?: 0
        if (assignments > 0) {
            return SpeakerRole.PARTICIPANT
        }

        return SpeakerRole.UNKNOWN
    }

    companion object {
        private val SPEAKER_PATTERN = Regex("^([A-Za-z\\s]+):\\s*")
        private val ASSIGNMENT_PATTERN = Regex("^(\\w{2,}),?\\s+(?:please|will you|can you|could you|should)")
        private val AT_MENTION_PATTERN = Regex("@(\\w+)")
        private val ASSIGNMENT_KEYWORDS = listOf("please", "will you", "can you", "could you", "need you to")
    }
}
