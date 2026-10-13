package com.jo.payonapp.intent

sealed class SignInIntent {
    data class UserNameChanged(val userName: String) : SignInIntent()
    data class PasswordChanged(val password: String) : SignInIntent()
    object SignInClicked : SignInIntent()

}