package com.jo.payonapp.sideeffect

sealed class MerchantSideEffect {
    data class ShowToast(val message: String) : MerchantSideEffect()
    object ReceiptCreatedSuccessfully : MerchantSideEffect()
}