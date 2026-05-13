package com.procrastinationkiller.domain.engine.transcript

/**
 * Shared regex patterns for transcript parsing. All transcript-related classes
 * should reference these constants to prevent pattern drift across files.
 */
object TranscriptPatterns {
    /**
     * Matches speaker lines in transcripts, supporting formats:
     * - "Speaker Name:"
     * - "[00:59:38] Speaker Name:"
     * - "[00:59:38] **Speaker 2:**"
     * - "00:59:38 - Speaker Name:"
     *
     * Captures the speaker name in group 1 (without surrounding asterisks or timestamps).
     */
    val SPEAKER_PATTERN = Regex("^(?:\\[?\\d{1,2}[:.\\-]\\d{2}(?:[:.\\-]\\d{2})?\\]?\\s*[-]?\\s*)?\\*{0,2}([A-Za-z][A-Za-z0-9\\s]*?)\\*{0,2}\\s*:\\s*")
}
