package com.jo.payonapp.state

import com.jo.payonapp.dto.AuthResponse

data class ContactsState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOfflineMode: Boolean = false,
    val contacts: List<AuthResponse> = emptyList(),
    val filteredContacts: List<AuthResponse> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
)