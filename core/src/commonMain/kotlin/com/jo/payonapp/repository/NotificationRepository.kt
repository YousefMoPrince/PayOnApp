package com.jo.payonapp.repository

import com.jo.payonapp.dto.NotificationResponse
import kotlinx.coroutines.flow.SharedFlow

interface NotificationRepository {
    suspend fun getNotification(id: String): Result<NotificationResponse>
    suspend fun getNotifications(page: Int, size: Int): Result<List<NotificationResponse>>
    suspend fun getUnreadNotifications(): Result<Long>
    suspend fun readNotification(notificationId: String): Result<Unit>
    suspend fun readAllNotification(): Result<Unit>

    fun startNotificationListener()
    fun observeRealTimeNotifications(): SharedFlow<NotificationResponse>
    fun observeConnectionState(): SharedFlow<Boolean>
    fun stopNotificationListener()
}