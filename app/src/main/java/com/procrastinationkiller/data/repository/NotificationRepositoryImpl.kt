package com.procrastinationkiller.data.repository

import com.procrastinationkiller.data.local.dao.NotificationDao
import com.procrastinationkiller.data.local.entity.NotificationEntity
import com.procrastinationkiller.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {
    override fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    override fun getUnprocessedNotifications(): Flow<List<NotificationEntity>> = notificationDao.getUnprocessedNotifications()
    override suspend fun getNotificationById(id: Long): NotificationEntity? = notificationDao.getNotificationById(id)
    override suspend fun insertNotification(notification: NotificationEntity): Long = notificationDao.insertNotification(notification)
    override suspend fun updateNotification(notification: NotificationEntity) = notificationDao.updateNotification(notification)
}
