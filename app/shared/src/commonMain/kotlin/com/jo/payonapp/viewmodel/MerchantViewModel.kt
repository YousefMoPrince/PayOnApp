package com.jo.payonapp.viewmodel

import com.jo.payonapp.dto.BecomeMerchantRequest
import com.jo.payonapp.dto.CreateReceiptRequest
import com.jo.payonapp.intent.MerchantIntent
import com.jo.payonapp.repository.MerchantRepository
import com.jo.payonapp.sideeffect.MerchantSideEffect
import com.jo.payonapp.state.MerchantState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MerchantViewModel(
    private val repository: MerchantRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(MerchantState())
    val state: StateFlow<MerchantState> = _state.asStateFlow()

    private val _sideEffect = Channel<MerchantSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        processIntent(MerchantIntent.FetchMerchantData)
    }

    fun processIntent(intent: MerchantIntent) {
        when (intent) {
            is MerchantIntent.FetchMerchantData -> fetchMerchantData(isRefresh = false)
            is MerchantIntent.RefreshMerchantData -> fetchMerchantData(isRefresh = true)
            is MerchantIntent.MerchantUserNameChanged -> _state.update { it.copy(merchantUserName = intent.merchantUserName) }
            is MerchantIntent.MerchantAccountNumberChanged -> _state.update { it.copy(merchantAccountNumber = intent.merchantAccountNumber) }
            is MerchantIntent.SubmitBecomeMerchant -> submitBecomeMerchant()
            is MerchantIntent.CreateReceipt -> createReceipt(intent.request)
            is MerchantIntent.FetchReceipts -> fetchReceipts()
        }
    }

    private fun fetchMerchantData(isRefresh: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = if (isRefresh) {
                repository.refreshMerchantData()
            } else {
                repository.getMerchantData()
            }

            result.onSuccess { merchantData ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isMerchant = true,
                        merchantUserName = merchantData.merchantUserName ?: "",
                        merchantAccountNumber = merchantData.accountNumber ?: 0L
                    )
                }
                fetchReceipts()
            }.onFailure {
                _state.update { it.copy(isLoading = false, isMerchant = false) }
            }
        }
    }

    private fun submitBecomeMerchant() {
        val currentUserName = _state.value.merchantUserName.trim()

        if (currentUserName.isEmpty()) {
            viewModelScope.launch { _sideEffect.send(MerchantSideEffect.ShowToast("Please enter merchant name")) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val request = BecomeMerchantRequest(merchantUserName = currentUserName)

            repository.becomeMerchant(request)
                .onSuccess {
                    _sideEffect.send(MerchantSideEffect.ShowToast("Congratulations! You are now a Merchant."))
                    fetchMerchantData(isRefresh = true)
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Registration failed"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                    _sideEffect.send(MerchantSideEffect.ShowToast(errorMsg))
                }
        }
    }

    private fun createReceipt(request: CreateReceiptRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.createReceipt(request)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(MerchantSideEffect.ShowToast("Receipt created successfully"))
                    _sideEffect.send(MerchantSideEffect.ReceiptCreatedSuccessfully)
                    fetchReceipts()
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to create receipt"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                    _sideEffect.send(MerchantSideEffect.ShowToast(errorMsg))
                }
        }
    }

    private fun fetchReceipts() {
        viewModelScope.launch {
            repository.getMerchantReceipts()
                .onSuccess { receiptList ->
                    _state.update { it.copy(receipts = receiptList) }
                }
                .onFailure {
                    _sideEffect.send(MerchantSideEffect.ShowToast("Receipts Not Found"))
                }
        }
    }


}