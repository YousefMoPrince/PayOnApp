package com.jo.payonapp.sideeffect

sealed class SignInSideEffect {
    object NavigateToMainActivity : SignInSideEffect()
    data class ShowToast(val message: String) : SignInSideEffect()
}