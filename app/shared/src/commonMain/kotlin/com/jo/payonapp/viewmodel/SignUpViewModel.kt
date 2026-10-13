package com.jo.payonapp.viewmodel

import com.jo.payonapp.dto.RegisterRequest
import com.jo.payonapp.intent.SignUpIntent
import com.jo.payonapp.repository.AuthRepository
import com.jo.payonapp.repository.WalletRepository
import com.jo.payonapp.sideeffect.SignUpSideEffect
import com.jo.payonapp.state.SignUpState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val authRepository: AuthRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state.asStateFlow()

    private val _sideEffect = Channel<SignUpSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun processIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.UsernameChanged -> _state.update { it.copy(username = intent.username) }
            is SignUpIntent.FullNameChanged -> _state.update { it.copy(fullName = intent.fullName) }
            is SignUpIntent.EmailChanged -> _state.update { it.copy(email = intent.email) }
            is SignUpIntent.PhoneNumberChanged -> _state.update { it.copy(phoneNumber = intent.phone) }
            is SignUpIntent.PasswordChanged -> _state.update { it.copy(password = intent.password) }
            is SignUpIntent.VerifyPasswordChanged -> _state.update { it.copy(verifyPassword = intent.verifyPassword) }
            is SignUpIntent.SignUpClicked -> signUp()
        }
    }

    private fun signUp() {
        val currentState = _state.value

        if (currentState.username.isBlank() || currentState.fullName.isBlank() ||
            currentState.email.isBlank() || currentState.phoneNumber.isBlank() ||
            currentState.password.isBlank()) {
            _state.update { it.copy(errorMessage = "All fields are required") }
            return
        }

        if (currentState.password != currentState.verifyPassword) {
            _state.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val request = RegisterRequest(
                username = currentState.username,
                fullName = currentState.fullName,
                email = currentState.email,
                phoneNumber = currentState.phoneNumber,
                password = currentState.password
            )

            authRepository.register(request)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(SignUpSideEffect.NavigateToUploadThumbnail)
                    createWallet()
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Sign up failed"
                    _state.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                    _sideEffect.send(SignUpSideEffect.ShowToast(errorMsg))
                }
        }
    }
    private fun createWallet(){
        viewModelScope.launch {
            walletRepository.createWallet()
        }
    }
}
