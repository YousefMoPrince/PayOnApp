package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class MerchantResponse(
    @SerialName("merchantId")
    val merchantId: String,

    @SerialName("merchantUserName")
    val merchantUserName: String,

    @SerialName("merchantUrl")
    val merchantUrl: String?,

    @SerialName("accountNumber")
    val accountNumber: Long,

    @SerialName("merchantType")
    val merchantType: MerchantType
)