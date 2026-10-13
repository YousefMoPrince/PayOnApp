package com.jo.payonapp.viewmodel

import com.jo.payonapp.dto.LoginRequest
import com.jo.payonapp.intent.SignInIntent
import com.jo.payonapp.repository.AuthRepository
import com.jo.payonapp.repository.WalletRepository
import com.jo.payonapp.sideeffect.SignInSideEffect
import com.jo.payonapp.state.SignInState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    private val authRepository: AuthRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    private val _sideEffect = Channel<SignInSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun processIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.UserNameChanged -> {
                _state.update { it.copy(username = intent.userName, errorMessage = null) }
            }
            is SignInIntent.PasswordChanged -> {
                _state.update { it.copy(password = intent.password, errorMessage = null) }
            }
            is SignInIntent.SignInClicked -> signIn()
        }
    }

    private fun signIn() {
        val currentState = _state.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter Username and Password") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val request = LoginRequest(currentState.username, currentState.password)

            authRepository.login(request)
                .onSuccess {
                    refreshWalletInfo()
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(SignInSideEffect.NavigateToMainActivity)
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Sign in failed"
                    _state.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                    _sideEffect.send(SignInSideEffect.ShowToast(errorMsg))
                }
        }
    }
    private fun refreshWalletInfo(){
        viewModelScope.launch {
            walletRepository.refreshWalletInfo()
        }
    }
}