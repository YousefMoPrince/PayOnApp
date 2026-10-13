package com.jo.payonapp.reposervice

import com.jo.payonapp.api.NotificationManager
import com.jo.payonapp.dto.NotificationResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.NotificationRepository
import com.jo.payonapp.service.NotificationWebSocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class NotificationRepositoryImpl(
    private val apiService: PayOnApiService,
    private val wsService: NotificationWebSocketService,
    private val notificationManager: NotificationManager
) : NotificationRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            wsService.notificationsFlow.collect { notification ->
                notificationManager.saveNotification(notification)
            }
        }
    }

    override suspend fun getNotification(id: String): Result<NotificationResponse> {
        return runCatching {
            val response = apiService.getNotification(id)
            notificationManager.saveNotification(response)
            response
        }
    }

    override suspend fun getNotifications(page: Int, size: Int): Result<List<NotificationResponse>> {
        return runCatching {
            val response = apiService.getNotifications(page, size)
            val notifications = response.content
            notificationManager.saveNotifications(notifications)
            notifications
        }.recoverCatching {
            notificationManager.getNotifications()
        }
    }

    override suspend fun getUnreadNotifications(): Result<Long> {
        return runCatching {
            apiService.getUnreadNotifications()
        }
    }

    override suspend fun readNotification(notificationId: String): Result<Unit> {
        return runCatching {
            apiService.readNotification(notificationId)
            notificationManager.markAsRead(notificationId)
        }
    }

    override suspend fun readAllNotification(): Result<Unit> {
        return runCatching {
            apiService.readAllNotification()
            notificationManager.markAllAsRead()
        }
    }

    override fun startNotificationListener() = wsService.connect()
    override fun observeRealTimeNotifications(): SharedFlow<NotificationResponse> = wsService.notificationsFlow
    override fun observeConnectionState(): SharedFlow<Boolean> = wsService.connectionStateFlow
    override fun stopNotificationListener() = wsService.disconnect()
}