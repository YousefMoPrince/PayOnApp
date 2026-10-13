package com.jo.payonapp.viewmodel

import com.jo.payonapp.intent.UploadThumbnailIntent
import com.jo.payonapp.repository.AuthRepository
import com.jo.payonapp.sideeffect.UploadThumbnailSideEffect
import com.jo.payonapp.state.UploadThumbnailState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadThumbnailViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(UploadThumbnailState())
    val state: StateFlow<UploadThumbnailState> = _state.asStateFlow()

    private val _sideEffect = Channel<UploadThumbnailSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun processIntent(intent: UploadThumbnailIntent) {
        when (intent) {
            is UploadThumbnailIntent.ImageSelected -> {
                _state.update { it.copy(selectedImageUri = intent.uriString) }
            }
            is UploadThumbnailIntent.UploadClicked -> {
                uploadImageAndProceed(intent.fileBytes, intent.fileName)
            }
            is UploadThumbnailIntent.SkipClicked -> {
                viewModelScope.launch {
                    _sideEffect.send(UploadThumbnailSideEffect.NavigateToMainActivity)
                }
            }
        }
    }

    private fun uploadImageAndProceed(fileBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    currentStatusMessage = "Uploading profile picture..."
                )
            }

            authRepository.uploadThumbnail(fileBytes, fileName)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(UploadThumbnailSideEffect.NavigateToMainActivity)
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Image upload failed. Try again."
                    _state.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                    _sideEffect.send(UploadThumbnailSideEffect.ShowToast(errorMsg))
                }
        }
    }
}