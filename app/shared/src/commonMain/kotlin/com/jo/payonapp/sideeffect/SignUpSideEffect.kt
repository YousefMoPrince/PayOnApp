package com.jo.payonapp.sideeffect

sealed class SignUpSideEffect {
    object NavigateToUploadThumbnail : SignUpSideEffect()
    data class ShowToast(val message: String) : SignUpSideEffect()
}