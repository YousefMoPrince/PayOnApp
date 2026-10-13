package com.jo.payonapp.state

import java.util.UUID

data class TransferState(
    val isLoading: Boolean = false,
    val transferAmount: Double = 0.0,
    val isTransferSuccess: Boolean = false,
    val isAccountVisible: Boolean = false,
    val receiverUsername: String = "",
    val accountNumber: Long = 0,
    val balance: Double = 0.0,
    val transactionId: String = "",
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val showConfirmDialog: Boolean = false,
    val error: String? = null
)