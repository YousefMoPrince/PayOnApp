package com.jo.payonapp.intent

import com.jo.payonapp.dto.Status
import java.math.BigDecimal

sealed class TransferIntent {
    object FetchWalletData : TransferIntent()
    object RefreshWalletData : TransferIntent()
    object ToggleAccountVisibility : TransferIntent()
    object TransferTransaction : TransferIntent()
    data class UpdateAmount(val amount: Double) : TransferIntent()
    data class UpdateReceiver(val username: String) : TransferIntent()
    data class TransferAcknowledgement(val status: Status) : TransferIntent()
}