package com.jo.payonapp.state

data class SignUpState(
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val verifyPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)