package com.procrastinationkiller.domain.repository

import com.procrastinationkiller.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    fun getUnprocessedNotifications(): Flow<List<NotificationEntity>>
    suspend fun getNotificationById(id: Long): NotificationEntity?
    suspend fun insertNotification(notification: NotificationEntity): Long
    suspend fun updateNotification(notification: NotificationEntity)
    suspend fun countProcessedByKey(key: String): Int
    suspend fun countBySbnKeyInLastHour(key: String, since: Long): Int
}
