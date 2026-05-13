package com.procrastinationkiller.domain.engine.semantic

import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemanticDeduplicator @Inject constructor() {

    companion object {
        private const val SIMILARITY_THRESHOLD = 0.75f
        private const val SAME_SENDER_BOOST = 0.0f
        private const val SHARED_ACTION_VERB_BOOST = 0.15f

        private val STOPWORDS = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will",
            "would", "could", "should", "may", "might", "shall", "can",
            "to", "of", "in", "for", "on", "with", "at", "by", "from",
            "it", "this", "that", "these", "those", "i", "you", "he",
            "she", "we", "they", "me", "him", "her", "us", "them",
            "my", "your", "his", "its", "our", "their", "and", "or",
            "but", "if", "then", "so", "please", "just", "also"
        )

        private val ACTION_VERBS = setOf(
            "send", "do", "complete", "finish", "submit", "review",
            "check", "call", "reply", "fix", "deploy", "write",
            "prepare", "update", "create", "forward", "buy", "pay",
            "make", "get", "bring", "take", "give", "tell",
            "schedule", "book", "order", "deliver", "approve", "confirm"
        )
    }

    fun checkDuplicate(
        newText: String,
        newSender: String,
        existingSuggestions: List<TaskSuggestionEntity>
    ): DeduplicationResult {
        val newTokens = tokenize(newText)
        val newActionVerbs = newTokens.filter { it in ACTION_VERBS }.toSet()

        var highestSimilarity = 0f
        var matchedTaskId: Long? = null

        for (suggestion in existingSuggestions) {
            val existingTokens = tokenize(suggestion.suggestedTitle)
            val existingActionVerbs = existingTokens.filter { it in ACTION_VERBS }.toSet()

            var similarity = jaccardSimilarity(newTokens, existingTokens)

            // Boost for same sender
            if (newSender.equals(suggestion.sender, ignoreCase = true)) {
                similarity += SAME_SENDER_BOOST
            }

            // Boost for shared action verbs
            val sharedVerbs = newActionVerbs.intersect(existingActionVerbs)
            if (sharedVerbs.isNotEmpty()) {
                similarity += SHARED_ACTION_VERB_BOOST
            }

            if (similarity > highestSimilarity) {
                highestSimilarity = similarity
                matchedTaskId = suggestion.id
            }
        }

        return DeduplicationResult(
            isDuplicate = highestSimilarity >= SIMILARITY_THRESHOLD,
            existingTaskId = if (highestSimilarity >= SIMILARITY_THRESHOLD) matchedTaskId else null,
            similarityScore = highestSimilarity
        )
    }

    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() && it !in STOPWORDS }
            .toSet()
    }

    private fun jaccardSimilarity(set1: Set<String>, set2: Set<String>): Float {
        if (set1.isEmpty() && set2.isEmpty()) return 0f
        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }
}
