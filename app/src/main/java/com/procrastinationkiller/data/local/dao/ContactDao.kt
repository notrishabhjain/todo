package com.procrastinationkiller.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.procrastinationkiller.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isEscalationTarget = 1")
    fun getEscalationContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE name = :name LIMIT 1")
    suspend fun getContactByName(name: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getContactByNameIgnoreCase(name: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE LOWER(name) LIKE '%' || LOWER(:name) || '%' OR LOWER(:name) LIKE '%' || LOWER(name) || '%' LIMIT 1")
    suspend fun getContactByNameFuzzy(name: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE priority = :priority ORDER BY name ASC")
    fun getContactsByPriority(priority: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE priority = 'VIP' ORDER BY name ASC")
    fun getVipContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE priority = 'IGNORE' ORDER BY name ASC")
    fun getIgnoredContacts(): Flow<List<ContactEntity>>

    @Query("UPDATE contacts SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: String)

    @Query("UPDATE contacts SET autoApprove = :autoApprove WHERE id = :id")
    suspend fun updateAutoApprove(id: Long, autoApprove: Boolean)

    @Query("UPDATE contacts SET messageCount = messageCount + 1, lastMessageTimestamp = :timestamp WHERE id = :id")
    suspend fun incrementMessageCount(id: Long, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)
}
