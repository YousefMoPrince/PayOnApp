package com.jo.payonapp.sideeffect

sealed class MainSideEffect {
    data class ShowToast(val message: String) : MainSideEffect()
    data class ShowNotificationBanner(val title: String, val eventType: String, val message: String) : MainSideEffect()
}