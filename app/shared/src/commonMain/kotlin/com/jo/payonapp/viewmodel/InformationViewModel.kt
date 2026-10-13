package com.jo.payonapp.viewmodel

import com.jo.payonapp.dto.UpdateRequest
import com.jo.payonapp.intent.InformationIntent
import com.jo.payonapp.network.NetworkConstants
import com.jo.payonapp.repository.AuthRepository
import com.jo.payonapp.repository.InformationRepository
import com.jo.payonapp.sideeffect.InformationSideEffect
import com.jo.payonapp.state.InformationState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InformationViewModel(
    private val informationRepository: InformationRepository,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(InformationState())
    val state: StateFlow<InformationState> = _state.asStateFlow()

    private val _sideEffect = Channel<InformationSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        processIntent(InformationIntent.FetchAccountData)
    }

    fun processIntent(intent: InformationIntent) {
        when (intent) {
            is InformationIntent.FetchAccountData -> fetchAccountData(isRefresh = false)
            is InformationIntent.RefreshData -> fetchAccountData(isRefresh = true)

            is InformationIntent.UsernameChanged -> _state.update { it.copy(draftUsername = intent.username) }
            is InformationIntent.FullNameChanged -> _state.update { it.copy(draftFullName = intent.fullName) }
            is InformationIntent.EmailChanged -> _state.update { it.copy(draftEmail = intent.email) }
            is InformationIntent.PhoneNumberChanged -> _state.update { it.copy(draftPhoneNumber = intent.phone) }

            is InformationIntent.ImageSelected -> {
                _state.update { it.copy(selectedImageUriString = intent.imageUriString) }
                viewModelScope.launch {
                    _sideEffect.send(InformationSideEffect.NavigateToUploadThumbnailSmallScreen)
                }
            }

            is InformationIntent.UploadClicked -> uploadThumbnail(intent.fileBytes, intent.fileName)
            is InformationIntent.UpdateData -> updateAccountData()
            is InformationIntent.LogOut -> performLogout()
        }
    }

    private fun fetchAccountData(isRefresh: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = if (isRefresh) {
                informationRepository.refreshUserProfile()
            } else {
                informationRepository.getUserProfile()
            }

            result.onSuccess { userData ->
                val rawPath = userData.thumbnailPath
                _state.update {
                    it.copy(
                        isLoading = false,
                        originalUsername = userData.userName ?: "",
                        originalFullName = userData.fullName ?: "",
                        originalEmail = userData.email ?: "",
                        originalPhoneNumber = userData.phoneNumber ?: "",
                        originalThumbnailPath = rawPath,
                        draftUsername = userData.userName ?: "",
                        draftFullName = userData.fullName ?: "",
                        draftEmail = userData.email ?: "",
                        draftPhoneNumber = userData.phoneNumber ?: "",
                        selectedImageUriString = null
                    )
                }

                if (!rawPath.isNullOrBlank()) {
                    loadLocalThumbnail(rawPath)
                }
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to fetch profile data"
                _state.update { it.copy(isLoading = false, error = errorMsg) }
                _sideEffect.send(InformationSideEffect.ShowToast(errorMsg))
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

            informationRepository.getDecryptedThumbnail(fileName = fileName, imageUrl = fullUrl)
                .onSuccess { bytes ->
                    _state.update { it.copy(profileImageBytes = bytes) }
                }
        }
    }

    private fun uploadThumbnail(fileBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            informationRepository.uploadThumbnail(fileBytes, fileName)
                .onSuccess { url ->
                    _state.update { it.copy(isLoading = false, originalThumbnailPath = url, selectedImageUriString = null) }
                    _sideEffect.send(InformationSideEffect.ShowToast("Image uploaded successfully!"))
                    authRepository.fetchProfileImageFromServer(url, fileName,)
                    fetchAccountData(isRefresh = true)
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Image upload failed"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                    _sideEffect.send(InformationSideEffect.ShowToast(errorMsg))
                }
        }
    }

    private fun updateAccountData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val updateRequest = UpdateRequest(
                username = _state.value.draftUsername,
                fullName = _state.value.draftFullName,
                email = _state.value.draftEmail,
                phoneNumber = _state.value.draftPhoneNumber,
                password = _state.value.draftPassword
            )

            informationRepository.updateProfile(updateRequest)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isUpdateSuccess = true) }
                    _sideEffect.send(InformationSideEffect.ShowToast("Profile updated successfully! Securing account..."))
                    performLogout()
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to update profile"
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                    _sideEffect.send(InformationSideEffect.ShowToast(errorMsg))
                }
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            informationRepository.logout()
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(InformationSideEffect.NavigateToSignInActivity)
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(InformationSideEffect.NavigateToSignInActivity)
                }
        }
    }
}