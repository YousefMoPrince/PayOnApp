package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class GeneralApiResponse<T>(
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: T? = null
)
