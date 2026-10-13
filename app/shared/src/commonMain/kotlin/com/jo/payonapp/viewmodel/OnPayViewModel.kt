package com.jo.payonapp.viewmodel

import com.jo.payonapp.intent.OnPayIntent
import com.jo.payonapp.repository.OnPayRepository
import com.jo.payonapp.sideeffect.OnPaySideEffect
import com.jo.payonapp.state.OnPayState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

class OnPayViewModel(
    private val onPayRepository: OnPayRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(OnPayState())
    val state: StateFlow<OnPayState> = _state.asStateFlow()

    private val _sideEffect = Channel<OnPaySideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        processIntent(OnPayIntent.FetchWalletData)
    }

    fun processIntent(intent: OnPayIntent) {
        when (intent) {
            is OnPayIntent.FetchWalletData -> fetchWalletData(isRefresh = false)
            is OnPayIntent.RefreshWalletData -> fetchWalletData(isRefresh = true)
            is OnPayIntent.ToggleAccountVisibility -> toggleOnPayCardVisibility()
            is OnPayIntent.UpdateAmount -> updateAmount(intent.amount)
            is OnPayIntent.UpdateTransactionCode -> updateTransactionCode(intent.code)
            is OnPayIntent.FetchReceipt -> fetchReceipt()
            is OnPayIntent.PayReceipt -> payReceipt()
        }
    }

    private fun toggleOnPayCardVisibility() {
        _state.update { it.copy(isAccountVisible = !it.isAccountVisible) }
    }

    private fun updateAmount(amount: Double) {
        _state.update { it.copy(amount = amount, error = null) }
    }

    private fun updateTransactionCode(code: Long) {
        _state.update { it.copy(transactionCode = code, error = null) }
    }

    private fun fetchWalletData(isRefresh: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = if (isRefresh) {
                onPayRepository.refreshWalletData()
            } else {
                onPayRepository.getWalletData()
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
                _sideEffect.send(OnPaySideEffect.ShowToast(errorMsg))
            }
        }
    }

    private fun fetchReceipt() {
        val currentState = _state.value

        if (currentState.transactionCode <= 0L) {
            viewModelScope.launch {
                _sideEffect.send(OnPaySideEffect.ShowToast("Invalid Transaction Code"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            onPayRepository.getReceipt(currentState.transactionCode)
                .onSuccess { receiptData ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            transactionId = receiptData.transactionId ?: "",
                            merchantUserName = receiptData.merchantUserName ?: "",
                            amount = receiptData.amount ?: 0.0,
                            currency = receiptData.currency ?: "",
                            transactionCode = receiptData.transactionCode ?: currentState.transactionCode,
                            error = null
                        )
                    }
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to fetch Receipt data"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                    _sideEffect.send(OnPaySideEffect.ShowToast(errorMsg))
                }
        }
    }

    private fun payReceipt() {
        val currentState = _state.value

        if (currentState.amount > currentState.balance) {
            viewModelScope.launch {
                _sideEffect.send(OnPaySideEffect.ShowToast("Insufficient Balance to pay this receipt."))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            onPayRepository.payReceipt(currentState.transactionCode)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _sideEffect.send(OnPaySideEffect.NavigateToSuccessFullReceiptScreen)
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to pay Receipt"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                    _sideEffect.send(OnPaySideEffect.NavigateToFailedScreen)
                    _sideEffect.send(OnPaySideEffect.ShowToast(errorMsg))
                }
        }
    }
}