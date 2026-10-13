package com.jo.payonapp.sideeffect

sealed class AccountSideEffect {
    object NavigateToSignInActivity : AccountSideEffect()
    data class ShowToast(val message: String) : AccountSideEffect()
}