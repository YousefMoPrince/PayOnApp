package com.jo.payonapp.sideeffect

sealed class InformationSideEffect {
    object NavigateToSignInActivity : InformationSideEffect()
    object NavigateToUploadThumbnailSmallScreen : InformationSideEffect()
    data class ShowToast(val message: String) : InformationSideEffect()
}