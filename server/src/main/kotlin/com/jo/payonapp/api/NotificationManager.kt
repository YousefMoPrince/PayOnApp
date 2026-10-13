package com.jo.payonapp.api

import com.jo.payonapp.dto.NotificationResponse

interface NotificationManager {
    fun saveNotifications(notifications: List<NotificationResponse>)
    fun saveNotification(notification: NotificationResponse)
    fun getNotifications(): List<NotificationResponse>
    fun markAsRead(id: String)
    fun markAllAsRead()
    fun clearNotifications()
}