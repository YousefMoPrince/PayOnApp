package com.jo.payonapp.state

import com.jo.payonapp.dto.NotificationResponse
import com.jo.payonapp.dto.TransactionResponse

data class MainState(
    val isLoading: Boolean = true,
    val username: String = "",
    val balance: String = "0.00",
    val accountNumber: String = "",
    val isAccountVisible: Boolean = false,
    val transactions: List<TransactionResponse> = emptyList(),
    val selectedTransaction: TransactionResponse? = null,
    val isAllTransactionsVisible: Boolean = false,
    val error: String? = null,
    val isLoadingNotifications: Boolean = false,
    val notificationsList: List<NotificationResponse> = emptyList(),
    val unreadNotificationsCount: Int = 0,
    val notificationsError: String? = null
) {
    val recentTransactions: List<TransactionResponse>
        get() = transactions.take(5)
}