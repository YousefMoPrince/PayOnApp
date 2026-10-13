package com.jo.payonapp.sideeffect

sealed class TransferSideEffect {
    object NavigateToSuccessFullTransferScreen : TransferSideEffect()
    object NavigateToFailedScreen : TransferSideEffect()
    object NavigateToMainActivity : TransferSideEffect()
    data class ShowToast(val message: String) : TransferSideEffect()
}