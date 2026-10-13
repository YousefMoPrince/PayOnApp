package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class RegisterRequest(
    @SerialName("username")
    val username: String,

    @SerialName("email")
    val email: String,

    @SerialName("password")
    val password: String,

    @SerialName("fullname")
    val fullName: String,

    @SerialName("phonenumber")
    val phoneNumber: String,

    @SerialName("thumbnailpath")
    val thumbnailPath: String? = null,

    @SerialName("createdat")
    val createdAt: String? = null
)