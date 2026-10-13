package com.jo.payonapp.reposervice

import com.jo.payonapp.api.NotificationManager
import com.jo.payonapp.dto.NotificationResponse
import com.jo.payonapp.local.PayOnDatabase

class SqliteNotificationManager(
    database: PayOnDatabase
) : NotificationManager {
    private val queries = database.notificationQueries

    override fun saveNotifications(notifications: List<NotificationResponse>) {
        queries.transaction {
            notifications.forEach { saveNotification(it) }
        }
    }

    override fun saveNotification(notification: NotificationResponse) {
        queries.insertOrReplaceNotification(
            id = notification.id ?: "",
            eventType = notification.eventType,
            title = notification.title,
            message = notification.message,
            isRead = if (notification.isRead) 1L else 0L,
            timestamp = notification.timestamp
        )
    }

    override fun getNotifications(): List<NotificationResponse> {
        return queries.getNotifications().executeAsList().map { entity ->
            NotificationResponse(
                id = entity.id,
                eventType = entity.eventType,
                title = entity.title,
                message = entity.message,
                isRead = entity.isRead == 1L,
                timestamp = entity.timestamp
            )
        }
    }

    override fun markAsRead(id: String) {
        queries.markAsRead(id)
    }

    override fun markAllAsRead() {
        queries.markAllAsRead()
    }

    override fun clearNotifications() {
        queries.clearNotifications()
    }
}