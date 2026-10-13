package com.jo.payonapp.sideeffect

sealed class DepositSideEffect {
    object NavigateToSuccessFullDepositScreen : DepositSideEffect()
    object NavigateToFailedScreen : DepositSideEffect()
    object NavigateToMainActivity : DepositSideEffect()
    data class ShowToast(val message: String) : DepositSideEffect()
}