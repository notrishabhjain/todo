package com.procrastinationkiller.domain.repository

import com.procrastinationkiller.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getAllContacts(): Flow<List<ContactEntity>>
    fun getEscalationContacts(): Flow<List<ContactEntity>>
    suspend fun getContactById(id: Long): ContactEntity?
    suspend fun getContactByName(name: String): ContactEntity?
    fun getContactsByPriority(priority: String): Flow<List<ContactEntity>>
    suspend fun updatePriority(id: Long, priority: String)
    suspend fun updateAutoApprove(id: Long, autoApprove: Boolean)
    suspend fun incrementMessageCount(id: Long)
    suspend fun insertContact(contact: ContactEntity): Long
    suspend fun updateContact(contact: ContactEntity)
    suspend fun deleteContact(contact: ContactEntity)
}
