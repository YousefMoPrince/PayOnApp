package com.jo.payonapp.state

import com.jo.payonapp.dto.CreatedReceiptResponse

data class MerchantState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isMerchant: Boolean = false,
    val merchantUserName: String = "",
    val merchantAccountNumber: Long = 0L,
    val receipts: List<CreatedReceiptResponse> = emptyList()
)