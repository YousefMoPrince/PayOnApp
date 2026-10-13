package com.jo.payonapp.intent

import java.math.BigDecimal

sealed class DepositIntent {
    object FetchWalletData : DepositIntent()
    object RefreshWalletData : DepositIntent()
    object ToggleAccountVisibility : DepositIntent()
    object DepositTransaction : DepositIntent()
    data class UpdateAmount(val amount: Double) : DepositIntent()
}