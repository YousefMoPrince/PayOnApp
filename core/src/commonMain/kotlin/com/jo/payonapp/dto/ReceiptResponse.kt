package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptResponse(
    @SerialName("transactionId")
    val transactionId: String,

    @SerialName("merchantUserName")
    val merchantUserName: String,

    @SerialName("amount")
    val amount: Double,

    @SerialName("currency")
    val currency: String,

    @SerialName("transactionCode")
    val transactionCode: Long
)