package com.jo.payonapp.viewmodel

import com.jo.payonapp.dto.TransactionRequest
import com.jo.payonapp.intent.WithdrawIntent
import com.jo.payonapp.repository.WalletRepository
import com.jo.payonapp.repository.WithdrawRepository
import com.jo.payonapp.sideeffect.WithdrawSideEffect
import com.jo.payonapp.state.WithdrawState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

class WithdrawViewModel(
    private val withdrawRepository: WithdrawRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(WithdrawState())
    val state: StateFlow<WithdrawState> = _state.asStateFlow()

    private val _sideEffect = Channel<WithdrawSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        processIntent(WithdrawIntent.FetchWalletData)
    }

    fun processIntent(intent: WithdrawIntent) {
        when (intent) {
            is WithdrawIntent.FetchWalletData -> fetchWalletData(isRefresh = false)
            is WithdrawIntent.RefreshWalletData -> fetchWalletData(isRefresh = true)
            is WithdrawIntent.ToggleAccountVisibility -> toggleWithdrawCardVisibility()
            is WithdrawIntent.UpdateAmount -> updateAmount(intent.amount)
            is WithdrawIntent.WithdrawTransaction -> executeWithdraw()
        }
    }

    private fun toggleWithdrawCardVisibility() {
        _state.update { it.copy(isAccountVisible = !it.isAccountVisible) }
    }

    private fun updateAmount(amount: Double) {
        _state.update { it.copy(withdrawAmount = amount, error = null) }
    }

    private fun fetchWalletData(isRefresh: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = if (isRefresh) {
                withdrawRepository.refreshWalletData()
            } else {
                withdrawRepository.getWalletData()
            }

            result.onSuccess { walletData ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        accountNumber = walletData.accountNumber ?: 0,
                        balance =walletData.balance ?: 0.0
                    )
                }
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to fetch wallet data"
                _state.update { it.copy(isLoading = false, error = errorMsg) }
                _sideEffect.send(WithdrawSideEffect.ShowToast(errorMsg))
            }
        }
    }

    private fun executeWithdraw() {
        val currentState = _state.value

        if (currentState.withdrawAmount <= 0.0) {
            viewModelScope.launch {
                _sideEffect.send(WithdrawSideEffect.ShowToast("Please enter a valid amount"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val request = TransactionRequest(
                amount = currentState.withdrawAmount,
                idempotencyKey = currentState.idempotencyKey,
                receiverUsername = ""
            )

            withdrawRepository.withdrawTransaction(
                idempotencyKey = currentState.idempotencyKey,
                withdrawRequest = request
            ).onSuccess { transactionData ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isWithdrawSuccess = true,
                        transactionId = transactionData.transactionId ?: "UNKNOWN",
                        idempotencyKey = UUID.randomUUID().toString(),
                        withdrawAmount = 0.0
                    )
                }
                refreshWallet()
                _sideEffect.send(WithdrawSideEffect.NavigateToSuccessFullWithdrawScreen)
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Withdraw failed"
                _state.update { it.copy(isLoading = false, error = errorMsg) }
                _sideEffect.send(WithdrawSideEffect.NavigateToFailedScreen)
                _sideEffect.send(WithdrawSideEffect.ShowToast(errorMsg))
            }
        }
    }
    private fun refreshWallet(){
        viewModelScope.launch {
            walletRepository.refreshWalletInfo()
        }
    }
}