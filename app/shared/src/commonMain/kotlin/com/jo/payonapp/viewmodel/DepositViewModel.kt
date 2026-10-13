package com.jo.payonapp.viewmodel

import com.jo.payonapp.dto.TransactionRequest
import com.jo.payonapp.intent.DepositIntent
import com.jo.payonapp.repository.DepositRepository
import com.jo.payonapp.repository.WalletRepository
import com.jo.payonapp.sideeffect.DepositSideEffect
import com.jo.payonapp.state.DepositState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

class DepositViewModel(
    private val depositRepository: DepositRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(DepositState())
    val state: StateFlow<DepositState> = _state.asStateFlow()

    private val _sideEffect = Channel<DepositSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        processIntent(DepositIntent.FetchWalletData)
    }

    fun processIntent(intent: DepositIntent) {
        when (intent) {
            is DepositIntent.FetchWalletData -> fetchWalletData(isRefresh = false)
            is DepositIntent.RefreshWalletData -> fetchWalletData(isRefresh = true)
            is DepositIntent.ToggleAccountVisibility -> toggleDepositCardVisibility()
            is DepositIntent.UpdateAmount -> updateAmount(intent.amount)
            is DepositIntent.DepositTransaction -> executeDeposit()
        }
    }

    private fun toggleDepositCardVisibility() {
        _state.update { it.copy(isAccountVisible = !it.isAccountVisible) }
    }

    private fun updateAmount(amount: Double) {
        _state.update { it.copy(depositAmount = amount, error = null) }
    }

    private fun fetchWalletData(isRefresh: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = if (isRefresh) {
                walletRepository.refreshWalletInfo()
            } else {
                walletRepository.getWalletInfo()
            }

            result.onSuccess { walletData ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        accountNumber = walletData.accountNumber ?: 0,
                        balance = walletData.balance ?: 0.0
                    )
                }
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to fetch wallet data"
                _state.update { it.copy(isLoading = false, error = errorMsg) }
                _sideEffect.send(DepositSideEffect.ShowToast(errorMsg))
            }
        }
    }
    private fun refreshWallet(){
        viewModelScope.launch {
            walletRepository.refreshWalletInfo()
        }
    }
    private fun executeDeposit() {
        val currentState = _state.value

        if (currentState.depositAmount <= 0.0) {
            viewModelScope.launch {
                _sideEffect.send(DepositSideEffect.ShowToast("Please enter a valid amount"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val request = TransactionRequest(
                amount = currentState.depositAmount,
                idempotencyKey = currentState.idempotencyKey,
                receiverUsername = ""
            )

            depositRepository.depositTransaction(
                idempotencyKey = currentState.idempotencyKey,
                depositRequest = request
            ).onSuccess { transactionData ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isDepositSuccess = true,
                        transactionId = transactionData.transactionId ?: "UNKNOWN",
                        idempotencyKey = UUID.randomUUID().toString(),
                        depositAmount = 0.0
                    )
                }
                refreshWallet()
                _sideEffect.send(DepositSideEffect.NavigateToSuccessFullDepositScreen)
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Deposit failed"
                _state.update { it.copy(isLoading = false, error = errorMsg) }
                _sideEffect.send(DepositSideEffect.NavigateToFailedScreen)
                _sideEffect.send(DepositSideEffect.ShowToast(errorMsg))
            }
        }
    }
}