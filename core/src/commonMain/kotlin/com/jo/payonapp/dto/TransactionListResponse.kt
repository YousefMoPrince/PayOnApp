package com.jo.payonapp.dto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class TransactionListResponse(
    @SerialName("message")
    val message: String,
    @SerialName("data")
    val data: List<TransactionResponse>
)