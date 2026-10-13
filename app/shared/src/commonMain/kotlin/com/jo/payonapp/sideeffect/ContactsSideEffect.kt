package com.jo.payonapp.sideeffect

sealed class ContactsSideEffect {
    data class ShowToast(val message: String) : ContactsSideEffect()
}