package com.jo.payonapp.intent

import java.math.BigDecimal

sealed class WithdrawIntent {
    object FetchWalletData : WithdrawIntent()
    object RefreshWalletData : WithdrawIntent()
    object ToggleAccountVisibility : WithdrawIntent()
    object WithdrawTransaction : WithdrawIntent()
    data class UpdateAmount(val amount: Double) : WithdrawIntent()
}