package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class NotificationResponse(
    @SerialName("id")
    val id: String? = null,

    @SerialName("eventType")
    val eventType: String,

    @SerialName("title")
    val title: String,

    @SerialName("message")
    val message: String,

    @SerialName("isRead")
    val isRead: Boolean = false,

    @SerialName("timestamp")
    val timestamp: String
)