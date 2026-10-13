package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class BecomeMerchantRequest(
    @SerialName("merchantUserName")
    val merchantUserName: String
)