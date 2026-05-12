package com.procrastinationkiller.domain.engine.semantic

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationFlowAnalyzer @Inject constructor() {

    companion object {
        private const val MAX_MESSAGES_PER_SENDER = 5
        private const val MAX_SENDER_ENTRIES = 100
        private val PRONOUN_REFERENCES = setOf(
            "that thing", "it", "the same", "that", "this",
            "woh", "wo", "yeh", "ye", "wahi"
        )
        private val ACTION_VERBS = setOf(
            "send", "do", "complete", "finish", "submit", "review",
            "check", "call", "reply", "fix", "deploy", "write",
            "prepare", "update", "create", "forward", "buy", "pay"
        )
    }

    private val conversationHistory = ConcurrentHashMap<String, MutableList<MessageContext>>()
    private val accessOrder = java.util.concurrent.ConcurrentLinkedDeque<String>()

    fun addMessage(sender: String, text: String, timestamp: Long = System.currentTimeMillis()) {
        val key = sender.lowercase()
        val messages = conversationHistory.getOrPut(key) { mutableListOf() }

        val actionVerbs = extractActionVerbs(text)
        val context = MessageContext(
            text = text,
            sender = sender,
            timestamp = timestamp,
            actionVerbs = actionVerbs
        )

        synchronized(messages) {
            messages.add(context)
            if (messages.size > MAX_MESSAGES_PER_SENDER) {
                messages.removeAt(0)
            }
        }

        // Update LRU access order and evict if over capacity
        accessOrder.remove(key)
        accessOrder.addLast(key)
        evictIfNeeded()
    }

    fun resolveContext(sender: String, currentText: String): MessageContext? {
        val lowerText = currentText.lowercase()

        // Check if the current message contains pronoun references
        val hasReference = PRONOUN_REFERENCES.any { lowerText.contains(it) }
        if (!hasReference) return null

        val key = sender.lowercase()
        val messages = conversationHistory[key] ?: return null

        // Find the most recent actionable message from this sender
        synchronized(messages) {
            return messages.lastOrNull { it.actionVerbs.isNotEmpty() }
        }
    }

    fun getRecentMessages(sender: String): List<MessageContext> {
        val key = sender.lowercase()
        val messages = conversationHistory[key] ?: return emptyList()
        synchronized(messages) {
            return messages.toList()
        }
    }

    fun clear() {
        conversationHistory.clear()
        accessOrder.clear()
    }

    private fun evictIfNeeded() {
        while (conversationHistory.size > MAX_SENDER_ENTRIES) {
            val oldest = accessOrder.pollFirst() ?: break
            conversationHistory.remove(oldest)
        }
    }

    private fun extractActionVerbs(text: String): List<String> {
        val words = text.lowercase().split("\\s+".toRegex())
        return words.filter { it in ACTION_VERBS }
    }
}
