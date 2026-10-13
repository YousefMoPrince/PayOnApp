package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.UUID
@Serializable
data class CreatedReceiptResponse(
    @SerialName("event")
    val state: ReceiptStatus,
    @SerialName("data")
    val data: CreatedReceiptResponseData
)
@Serializable
data class CreatedReceiptResponseData(
    @SerialName("transactionId")
    val transactionId: String,
    @SerialName("clientName")
    val clientName: String,
    @SerialName("merchantUserName")
    val merchantUserName: String,
    @SerialName("merchantType")
    val merchantType: MerchantType,
    @SerialName("amount")
    val amount: Double,
    @SerialName("accountNumber")
    val accountNumber: Long,
    @SerialName("payerAccountNumber")
    val payerAccountNumber: Long? = null,
    @SerialName("currency")
    val currency: Currency,
    @SerialName("transactionCode")
    val transactionCode: Long,
    @SerialName("idempotencyKey")
    val idempotencyKey: String,
    @SerialName("createdAt")
    val createdAt: String,
    @SerialName("updatedAt")
    val updatedAt: String? = null
)