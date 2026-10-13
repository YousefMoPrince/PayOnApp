package com.jo.payonapp.sideeffect

sealed class OnPaySideEffect {
    object NavigateToSuccessFullReceiptScreen : OnPaySideEffect()
    object NavigateToFailedScreen : OnPaySideEffect()
    object NavigateToMainActivity : OnPaySideEffect()
    data class ShowToast(val message: String) : OnPaySideEffect()
}