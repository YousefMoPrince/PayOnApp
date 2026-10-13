package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    @SerialName("userId")
    val userId: String? = null,

    @SerialName("userName")
    val userName: String? = null,

    @SerialName("email")
    val email: String? = null,

    @SerialName("phoneNumber")
    val phoneNumber: String? = null,

    @SerialName("fullName")
    val fullName: String? = null,

    @SerialName("thumbnailPath")
    val thumbnailPath: String? = null,

    @SerialName("createdAt")
    val createdAt: String? = null,

    @SerialName("updatedAt")
    val updatedAt: String? = null,

    @SerialName("Token")
    val token: String? = null,

    @SerialName("refreshToken")
    val refreshToken: String? = null
) {
    val safeUsername: String
        get() = userName?.let { if (it.startsWith("@")) it else "@$it" } ?: "@user"

    val safePhone: String
        get() = phoneNumber ?: "No phone number"

    val safeEmail: String
        get() = email ?: "No email provided"
}