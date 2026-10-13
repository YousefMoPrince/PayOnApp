package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.UUID
@Serializable
data class TransactionResponse(
    @SerialName("transactionId")
    val transactionId: String,

    @SerialName("amount")
    val amount: Double,

    @SerialName("type")
    val type: String,

    @SerialName("receiverUsername")
    val receiverUsername: String?,

    @SerialName("status")
    val status: String,

    @SerialName("idempotencyKey")
    val idempotencyKey: String?,

    @SerialName("timestamp")
    val timestamp: String
)