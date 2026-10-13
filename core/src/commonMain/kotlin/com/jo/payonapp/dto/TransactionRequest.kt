package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class TransactionRequest(
    @SerialName("amount")
    val amount: Double,

    @SerialName("idempotencyKey")
    val idempotencyKey: String,

    @SerialName("receiverUsername")
    val receiverUsername: String? = null
)