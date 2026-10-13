package com.jo.payonapp.sideeffect

sealed class UploadThumbnailSideEffect {
    object NavigateToMainActivity : UploadThumbnailSideEffect()
    data class ShowToast(val message: String) : UploadThumbnailSideEffect()
}