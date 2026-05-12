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
    override suspend fun insertContact(contact: ContactEntity): Long = contactDao.insertContact(contact)
    override suspend fun updateContact(contact: ContactEntity) = contactDao.updateContact(contact)
    override suspend fun deleteContact(contact: ContactEntity) = contactDao.deleteContact(contact)
}
