package com.jo.payonapp.sideeffect

sealed class WithdrawSideEffect {
    object NavigateToSuccessFullWithdrawScreen : WithdrawSideEffect()
    object NavigateToFailedScreen : WithdrawSideEffect()
    object NavigateToMainActivity : WithdrawSideEffect()
    data class ShowToast(val message: String) : WithdrawSideEffect()
}