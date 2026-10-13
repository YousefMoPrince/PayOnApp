package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.UUID
@Serializable
data class CreateReceiptRequest(
    @SerialName("state")
    val state: ReceiptStatus,
    @SerialName("data")
    val data: CreateReceiptRequestData
)
@Serializable
data class CreateReceiptRequestData(
    @SerialName("amount")
    val amount: Double,
    @SerialName("clientName")
    val clientName: String,
    @SerialName("currency")
    val currency: Currency,
    @SerialName("idempotencyKey")
    val idempotencyKey: String
)
