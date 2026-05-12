package com.procrastinationkiller.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val isEscalationTarget: Boolean = false,
    val priority: String = "NORMAL",
    val autoApprove: Boolean = false,
    val sourceApp: String = "",
    val messageCount: Int = 0,
    val lastMessageTimestamp: Long? = null
)
