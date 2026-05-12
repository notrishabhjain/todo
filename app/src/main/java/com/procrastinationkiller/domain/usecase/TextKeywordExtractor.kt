package com.procrastinationkiller.domain.usecase

/**
 * Shared utility for extracting keywords from text, used by both
 * ApproveTaskUseCase and RejectTaskUseCase for learning feedback.
 */
object TextKeywordExtractor {

    private val STOP_WORDS = setOf(
        "this", "that", "with", "from", "have", "been", "were", "they",
        "them", "then", "than", "these", "those", "their", "there",
        "when", "what", "which", "where", "will", "would", "could",
        "should", "about", "after", "before", "between", "each",
        "every", "into", "through", "does", "done", "just", "more",
        "most", "much", "also", "back", "some", "such", "very",
        "your", "yours", "here", "only", "still", "over", "under"
    )

    fun extractKeywords(text: String): List<String> {
        return text.lowercase()
            .split("\\s+".toRegex())
            .filter { it.length > 3 && it !in STOP_WORDS }
            .take(10)
    }
}
