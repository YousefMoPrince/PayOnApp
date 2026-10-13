package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class RegisterUserDeviceRequest(
    @SerialName("deviceToken")
    val deviceToken: String,
    @SerialName("deviceType")
    val deviceType: String
)
