package com.jo.payonapp.viewmodel

import com.jo.payonapp.dto.Status
import com.jo.payonapp.dto.TransactionRequest
import com.jo.payonapp.intent.TransferIntent
import com.jo.payonapp.repository.TransferRepository
import com.jo.payonapp.repository.WalletRepository
import com.jo.payonapp.sideeffect.TransferSideEffect
import com.jo.payonapp.state.TransferState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID

class TransferViewModel(
    private val transferRepository: TransferRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(TransferState())
    val state: StateFlow<TransferState> = _state.asStateFlow()

    private val _sideEffect = Channel<TransferSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        processIntent(TransferIntent.FetchWalletData)
    }

    fun processIntent(intent: TransferIntent) {
        when (intent) {
            is TransferIntent.FetchWalletData -> fetchWalletData(isRefresh = false)
            is TransferIntent.RefreshWalletData -> fetchWalletData(isRefresh = true)
            is TransferIntent.ToggleAccountVisibility -> toggleTransferCardVisibility()
            is TransferIntent.UpdateAmount -> updateAmount(intent.amount)
            is TransferIntent.UpdateReceiver -> updateReceiver(intent.username)
            is TransferIntent.TransferTransaction -> executeTransfer()
            is TransferIntent.TransferAcknowledgement -> executeAcknowledgement(intent.status)
        }
    }

    private fun toggleTransferCardVisibility() {
        _state.update { it.copy(isAccountVisible = !it.isAccountVisible) }
    }

    private fun updateAmount(amount: Double) {
        _state.update { it.copy(transferAmount = amount, error = null) }
    }

    private fun updateReceiver(username: String) {
        _state.update { it.copy(receiverUsername = username, error = null) }
    }

    private fun fetchWalletData(isRefresh: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = if (isRefresh) {
                transferRepository.refreshWalletData()
            } else {
                transferRepository.getWalletData()
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
                _sideEffect.send(TransferSideEffect.ShowToast(errorMsg))
            }
        }
    }

    private fun executeTransfer() {
        val currentState = _state.value

        if (currentState.receiverUsername.isBlank()) {
            viewModelScope.launch {
                _sideEffect.send(TransferSideEffect.ShowToast("Please select or enter a valid receiver"))
            }
            return
        }

        if (currentState.transferAmount <= 0.0) {
            viewModelScope.launch {
                _sideEffect.send(TransferSideEffect.ShowToast("Please enter a valid amount"))
            }
            return
        }

        if (currentState.transferAmount > currentState.balance) {
            viewModelScope.launch {
                _sideEffect.send(TransferSideEffect.ShowToast("Insufficient balance"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentIdempotencyKey = UUID.randomUUID().toString()
            val request = TransactionRequest(
                amount = currentState.transferAmount,
                idempotencyKey = currentIdempotencyKey,
                receiverUsername = currentState.receiverUsername
            )

            transferRepository.transferTransaction(
                idempotencyKey = currentIdempotencyKey,
                transferRequest = request
            ).onSuccess { transactionData ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isTransferSuccess = false,
                        transactionId = transactionData.transactionId ?: "UNKNOWN",
                        idempotencyKey = currentIdempotencyKey,
                        showConfirmDialog = true
                    )
                }
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Transfer initiation failed"
                _state.update { it.copy(isLoading = false, error = errorMsg) }
                _sideEffect.send(TransferSideEffect.NavigateToFailedScreen)
                _sideEffect.send(TransferSideEffect.ShowToast(errorMsg))
            }
        }
    }

    private fun executeAcknowledgement(status: Status) {
        val currentState = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showConfirmDialog = false) }

            transferRepository.transferAcknowledgement(
                idempotencyKey = currentState.idempotencyKey,
                status = status
            ).onSuccess {
                if (status == Status.SUCCESS) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isTransferSuccess = true,
                            idempotencyKey = UUID.randomUUID().toString()
                        )
                    }
                    refreshWallet()
                    _sideEffect.send(TransferSideEffect.NavigateToSuccessFullTransferScreen)
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            idempotencyKey = UUID.randomUUID().toString()
                        )
                    }
                    _sideEffect.send(TransferSideEffect.ShowToast("Transfer Cancelled"))
                }
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Acknowledgement failed"
                _state.update { it.copy(isLoading = false, error = errorMsg) }
                _sideEffect.send(TransferSideEffect.NavigateToFailedScreen)
                _sideEffect.send(TransferSideEffect.ShowToast(errorMsg))
            }
        }
    }
    private fun refreshWallet(){
        viewModelScope.launch {
            walletRepository.refreshWalletInfo()
        }
    }
}