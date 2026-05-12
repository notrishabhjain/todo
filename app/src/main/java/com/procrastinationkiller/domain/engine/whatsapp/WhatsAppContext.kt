package com.procrastinationkiller.domain.engine.whatsapp

import com.procrastinationkiller.domain.model.ContactPriority

enum class ChatType {
    PERSONAL,
    GROUP
}

data class WhatsAppContext(
    val senderName: String,
    val messageSnippet: String,
    val timestamp: Long,
    val chatType: ChatType,
    val groupName: String? = null,
    val contactPriority: ContactPriority = ContactPriority.NORMAL
)
