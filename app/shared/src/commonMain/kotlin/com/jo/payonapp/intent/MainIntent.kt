package com.jo.payonapp.intent

import com.jo.payonapp.dto.TransactionResponse

sealed class MainIntent {
    object FetchHomeData : MainIntent()
    object ToggleAccountVisibility : MainIntent()
    object RefreshData : MainIntent()
    object FetchNotifications : MainIntent()
    object MarkNotificationsAsRead : MainIntent()

    data class SelectTransaction(val transaction: TransactionResponse?) : MainIntent()
    data class SetAllTransactionsVisible(val isVisible: Boolean) : MainIntent()
}