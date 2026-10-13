package com.jo.payonapp.state

data class AccountState(
    val isLoading: Boolean = false,
    val username: String = "",
    val phoneNumber: String = "",
    val thumbnailPath: String? = null,
    val profileImageBytes: ByteArray? = null,
    val error: String? = null
)