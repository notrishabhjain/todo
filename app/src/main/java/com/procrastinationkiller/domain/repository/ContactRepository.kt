package com.procrastinationkiller.domain.repository

import com.procrastinationkiller.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getAllContacts(): Flow<List<ContactEntity>>
    fun getEscalationContacts(): Flow<List<ContactEntity>>
    suspend fun getContactById(id: Long): ContactEntity?
    suspend fun insertContact(contact: ContactEntity): Long
    suspend fun updateContact(contact: ContactEntity)
    suspend fun deleteContact(contact: ContactEntity)
}
