package com.jo.payonapp.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class WalletResponse(
    @SerialName("WalletId")
    val walletId: String,

    @SerialName("userId")
    val userId: String,

    @SerialName("balance")
    val balance: Double,

    @SerialName("accountNumber")
    val accountNumber: Long,

    @SerialName("walletStatus")
    val walletStatus: WalletStatus
)