package com.jo.payonapp.intent

sealed class SignUpIntent {
    data class UsernameChanged(val username: String) : SignUpIntent()
    data class FullNameChanged(val fullName: String) : SignUpIntent()
    data class EmailChanged(val email: String) : SignUpIntent()
    data class PhoneNumberChanged(val phone: String) : SignUpIntent()
    data class PasswordChanged(val password: String) : SignUpIntent()
    data class VerifyPasswordChanged(val verifyPassword: String) : SignUpIntent()
    object SignUpClicked : SignUpIntent()
}