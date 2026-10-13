package com.jo.payonapp.state

import java.math.BigDecimal

data class OnPayState(
    val isLoading: Boolean = false,
    val accountNumber: Long = 0,
    val balance: Double = 0.0,
    val isAccountVisible: Boolean = false,
    val transactionCode: Long = 0L,
    val transactionId: String = "",
    val merchantUserName: String = "",
    val amount: Double = 0.0,
    val currency: String = "",
    val error: String? = null
)