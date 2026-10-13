package com.jo.payonapp.viewmodel

import com.jo.payonapp.intent.AccountIntent
import com.jo.payonapp.network.NetworkConstants
import com.jo.payonapp.repository.AccountRepository
import com.jo.payonapp.sideeffect.AccountSideEffect
import com.jo.payonapp.state.AccountState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val repository: AccountRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    private val _sideEffect = Channel<AccountSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        processIntent(AccountIntent.FetchAccountData)
    }

    fun processIntent(intent: AccountIntent) {
        when (intent) {
            is AccountIntent.FetchAccountData -> fetchAccountData(isRefresh = false)
            is AccountIntent.RefreshData -> fetchAccountData(isRefresh = true)
            is AccountIntent.LogOutClicked -> logOut()
        }
    }

    private fun fetchAccountData(isRefresh: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = if (isRefresh) {
                repository.refreshUserProfile()
            } else {
                repository.getUserProfile()
            }

            result.onSuccess { profile ->
                val thumbPath = profile.thumbnailPath
                _state.update {
                    it.copy(
                        isLoading = false,
                        username = profile.userName ?: "User",
                        phoneNumber = profile.phoneNumber ?: "",
                        thumbnailPath = thumbPath
                    )
                }

                if (!thumbPath.isNullOrBlank()) {
                    loadLocalThumbnail(thumbPath)
                }
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to load account data"
                _state.update { it.copy(isLoading = false, error = errorMsg) }
                _sideEffect.send(AccountSideEffect.ShowToast(errorMsg))
            }
        }
    }

    private fun loadLocalThumbnail(rawPath: String) {
        viewModelScope.launch {
            val fileName = rawPath.substringAfterLast("/")
            val fullUrl = if (!rawPath.startsWith("http")) {
                "${NetworkConstants.Base_Url}$rawPath"
            } else {
                rawPath
            }

            repository.getDecryptedThumbnail(fileName = fileName, imageUrl = fullUrl)
                .onSuccess { bytes ->
                    _state.update { it.copy(profileImageBytes = bytes) }
                }
        }
    }

    private fun logOut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            repository.logout()
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(AccountSideEffect.NavigateToSignInActivity)
                }
                .onFailure { exception ->
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(
                        AccountSideEffect.ShowToast(
                            exception.message ?: "Error during logout"
                        )
                    )
                }
        }
    }
}