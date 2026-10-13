package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class LogoutRequest(
    @SerialName("RefreshToken")
    val refreshToken: String
)