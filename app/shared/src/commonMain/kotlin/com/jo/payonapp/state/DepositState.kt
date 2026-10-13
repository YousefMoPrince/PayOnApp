package com.jo.payonapp.state

import java.math.BigDecimal
import java.util.UUID

data class DepositState(
    val isLoading: Boolean = false,
    val depositAmount: Double = 0.0,
    val isDepositSuccess: Boolean = false,
    val isAccountVisible: Boolean = false,
    val accountNumber: Long = 0,
    val balance: Double = 0.0,
    val transactionId: String = "",
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val error: String? = null
)