package com.procrastinationkiller.domain.engine.whatsapp

import com.procrastinationkiller.data.local.entity.ContactEntity
import com.procrastinationkiller.domain.model.ContactPriority
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.repository.ContactRepository
import javax.inject.Inject
import javax.inject.Singleton

sealed class WhatsAppEvaluationResult {
    data class IgnoreResult(val reason: String) : WhatsAppEvaluationResult()
    data class ProcessResult(
        val whatsAppContext: WhatsAppContext,
        val priorityOverride: TaskPriority?,
        val autoApprove: Boolean,
        val contactPriority: ContactPriority
    ) : WhatsAppEvaluationResult()
}

@Singleton
class WhatsAppIntelligenceEngine @Inject constructor(
    private val contactRepository: ContactRepository
) {

    suspend fun evaluate(
        sender: String,
        message: String,
        isGroupChat: Boolean,
        groupName: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): WhatsAppEvaluationResult {
        val contact = contactRepository.getContactByName(sender)
        val contactPriority = contact?.let { parseContactPriority(it.priority) } ?: ContactPriority.NORMAL

        // Update contact interaction tracking
        if (contact != null) {
            contactRepository.incrementMessageCount(contact.id)
        } else {
            // Create new contact on first interaction
            contactRepository.insertContact(
                ContactEntity(
                    name = sender,
                    sourceApp = "com.whatsapp",
                    messageCount = 1,
                    lastMessageTimestamp = timestamp
                )
            )
        }

        // If contact is set to IGNORE, skip task creation
        if (contactPriority == ContactPriority.IGNORE) {
            return WhatsAppEvaluationResult.IgnoreResult("Contact $sender is set to IGNORE priority")
        }

        val chatType = if (isGroupChat) ChatType.GROUP else ChatType.PERSONAL
        val snippet = if (message.length > 100) message.take(100) else message

        val whatsAppContext = WhatsAppContext(
            senderName = sender,
            messageSnippet = snippet,
            timestamp = timestamp,
            chatType = chatType,
            groupName = groupName,
            contactPriority = contactPriority
        )

        // VIP contacts get HIGH priority minimum
        val priorityOverride = when (contactPriority) {
            ContactPriority.VIP -> TaskPriority.HIGH
            else -> null
        }

        // Auto-approve for VIP contacts or if contact has autoApprove enabled
        val autoApprove = contactPriority == ContactPriority.VIP || contact?.autoApprove == true

        return WhatsAppEvaluationResult.ProcessResult(
            whatsAppContext = whatsAppContext,
            priorityOverride = priorityOverride,
            autoApprove = autoApprove,
            contactPriority = contactPriority
        )
    }

    private fun parseContactPriority(priority: String): ContactPriority {
        return try {
            ContactPriority.valueOf(priority)
        } catch (_: IllegalArgumentException) {
            ContactPriority.NORMAL
        }
    }
}
