package com.jo.payonapp.viewmodel

import com.jo.payonapp.dto.TransactionResponse
import com.jo.payonapp.intent.MainIntent
import com.jo.payonapp.repository.HomeRepository
import com.jo.payonapp.repository.NotificationRepository
import com.jo.payonapp.repository.WalletRepository
import com.jo.payonapp.sideeffect.MainSideEffect
import com.jo.payonapp.state.MainState
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: HomeRepository,
    private val notificationRepository: NotificationRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val _sideEffect = Channel<MainSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        processIntent(MainIntent.FetchHomeData)
        observeRealTimeNotifications()
    }

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.FetchHomeData, is MainIntent.RefreshData -> {
                fetchHomeData()
                fetchNotificationsHistory()
            }
            is MainIntent.ToggleAccountVisibility -> toggleVisibility()
            is MainIntent.FetchNotifications -> fetchNotificationsHistory()
            is MainIntent.MarkNotificationsAsRead -> clearUnreadCount()
            is MainIntent.SelectTransaction -> _state.update { it.copy(selectedTransaction = intent.transaction) }
            is MainIntent.SetAllTransactionsVisible -> _state.update { it.copy(isAllTransactionsVisible = intent.isVisible) }
        }
    }

    private fun toggleVisibility() {
        _state.update { it.copy(isAccountVisible = !it.isAccountVisible) }
    }

    private fun fetchHomeData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val profileDeferred = async { repository.getUserProfile() }
            val walletDeferred = async { walletRepository.getWalletInfo() }
            val transactionsDeferred = async { repository.getTransactions() }

            val profileRes = profileDeferred.await()
            val walletRes = walletDeferred.await()
            val transRes = transactionsDeferred.await()

            if (profileRes.isSuccess && walletRes.isSuccess && transRes.isSuccess) {
                val profile = profileRes.getOrNull()
                val wallet = walletRes.getOrNull()
                val transactions = transRes.getOrNull()?.reversed() ?: emptyList()

                _state.update {
                    it.copy(
                        isLoading = false,
                        username = profile?.userName ?: "User",
                        balance = wallet?.balance?.toString() ?: "0.00",
                        accountNumber = wallet?.accountNumber?.toString() ?: "",
                        transactions = transactions.reversed()
                    )
                }
            } else {
                val errorMsg = profileRes.exceptionOrNull()?.message
                    ?: walletRes.exceptionOrNull()?.message
                    ?: transRes.exceptionOrNull()?.message
                    ?: "Error loading home data"

                _state.update { it.copy(isLoading = false, error = errorMsg) }
                _sideEffect.send(MainSideEffect.ShowToast(errorMsg))
            }
        }
    }

    private fun fetchNotificationsHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingNotifications = true, notificationsError = null) }

            notificationRepository.getNotifications(page = 0, size = 20)
                .onSuccess { history ->
                    _state.update {
                        it.copy(
                            isLoadingNotifications = false,
                            notificationsList = history,
                            unreadNotificationsCount = history.count { notif -> !notif.isRead }
                        )
                    }
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to load notifications"
                    _state.update {
                        it.copy(
                            isLoadingNotifications = false,
                            notificationsError = errorMsg
                        )
                    }
                }
        }
    }

    private fun observeRealTimeNotifications() {
        viewModelScope.launch {
            notificationRepository.observeRealTimeNotifications().collect { newNotification ->
                _state.update { currentState ->
                    currentState.copy(
                        notificationsList = listOf(newNotification) + currentState.notificationsList,
                        unreadNotificationsCount = currentState.unreadNotificationsCount + 1
                    )
                }
                _sideEffect.send(
                    MainSideEffect.ShowNotificationBanner(
                        title = newNotification.title,
                        eventType = newNotification.eventType,
                        message = newNotification.message
                    )
                )
            }
        }
    }

    private fun clearUnreadCount() {
        viewModelScope.launch {
            notificationRepository.readAllNotification().onSuccess {
                _state.update { it.copy(unreadNotificationsCount = 0) }
            }
        }
    }
}