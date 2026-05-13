package com.procrastinationkiller.data.repository

import com.procrastinationkiller.data.local.dao.ContactDao
import com.procrastinationkiller.data.local.entity.ContactEntity
import com.procrastinationkiller.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao
) : ContactRepository {
    override fun getAllContacts(): Flow<List<ContactEntity>> = contactDao.getAllContacts()
    override fun getEscalationContacts(): Flow<List<ContactEntity>> = contactDao.getEscalationContacts()
    override suspend fun getContactById(id: Long): ContactEntity? = contactDao.getContactById(id)
    override suspend fun getContactByName(name: String): ContactEntity? = contactDao.getContactByName(name)
    override suspend fun getContactByNameIgnoreCase(name: String): ContactEntity? = contactDao.getContactByNameIgnoreCase(name)
    override suspend fun getContactByNameFuzzy(name: String): ContactEntity? = contactDao.getContactByNameFuzzy(name)
    override fun getContactsByPriority(priority: String): Flow<List<ContactEntity>> = contactDao.getContactsByPriority(priority)
    override suspend fun updatePriority(id: Long, priority: String) = contactDao.updatePriority(id, priority)
    override suspend fun updateAutoApprove(id: Long, autoApprove: Boolean) = contactDao.updateAutoApprove(id, autoApprove)
    override suspend fun incrementMessageCount(id: Long) = contactDao.incrementMessageCount(id)
    override suspend fun insertContact(contact: ContactEntity): Long = contactDao.insertContact(contact)
    override suspend fun updateContact(contact: ContactEntity) = contactDao.updateContact(contact)
    override suspend fun deleteContact(contact: ContactEntity) = contactDao.deleteContact(contact)
}
