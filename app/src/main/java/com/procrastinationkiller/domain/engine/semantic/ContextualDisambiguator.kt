package com.procrastinationkiller.domain.engine.semantic

import com.procrastinationkiller.domain.engine.learning.AdaptiveWeightManager
import com.procrastinationkiller.domain.model.ContactPriority
import com.procrastinationkiller.domain.model.TaskPriority
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextualDisambiguator @Inject constructor(
    private val adaptiveWeightManager: AdaptiveWeightManager
) {

    fun disambiguate(
        text: String,
        sender: String,
        contactPriority: ContactPriority?
    ): DisambiguationResult {
        val senderImportance = adaptiveWeightManager.getSenderImportance(sender)
        val effectivePriority = contactPriority ?: ContactPriority.NORMAL

        val adjustedPriority = when (effectivePriority) {
            ContactPriority.VIP -> TaskPriority.HIGH
            ContactPriority.HIGH_PRIORITY -> {
                if (senderImportance > 1.5f) TaskPriority.HIGH else TaskPriority.MEDIUM
            }
            ContactPriority.NORMAL -> TaskPriority.MEDIUM
            ContactPriority.IGNORE -> TaskPriority.LOW
        }

        return DisambiguationResult(
            adjustedPriority = adjustedPriority,
            senderContext = senderImportance
        )
    }
}
