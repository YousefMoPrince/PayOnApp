package com.jo.payonapp.intent

import java.math.BigDecimal

sealed class OnPayIntent {
    object FetchWalletData : OnPayIntent()
    object RefreshWalletData : OnPayIntent()
    object ToggleAccountVisibility : OnPayIntent()
    object FetchReceipt : OnPayIntent()
    object PayReceipt : OnPayIntent()
    data class UpdateAmount(val amount: Double) : OnPayIntent()
    data class UpdateTransactionCode(val code: Long) : OnPayIntent()
}