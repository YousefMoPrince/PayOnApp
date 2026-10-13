package com.jo.payonapp.viewmodel

import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.intent.ContactsIntent
import com.jo.payonapp.repository.ContactsRepository
import com.jo.payonapp.sideeffect.ContactsSideEffect
import com.jo.payonapp.state.ContactsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val repository: ContactsRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(ContactsState())
    val state: StateFlow<ContactsState> = _state.asStateFlow()

    private val _sideEffect = Channel<ContactsSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        processIntent(ContactsIntent.LoadCachedContacts)
    }

    fun processIntent(intent: ContactsIntent) {
        when (intent) {
            is ContactsIntent.LoadCachedContacts -> loadCachedContacts()
            is ContactsIntent.SyncContacts -> syncContacts(intent.phoneNumbers)
            is ContactsIntent.UpdateSearchQuery -> updateSearch(intent.query)
            is ContactsIntent.RefreshContacts -> {
                // Handled in UI to re-read device numbers and sync
            }
        }
    }

    private fun loadCachedContacts() {
        viewModelScope.launch {
            repository.getContacts().onSuccess { cached ->
                if (cached.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            contacts = cached,
                            filteredContacts = filterList(cached, it.searchQuery)
                        )
                    }
                }
            }
        }
    }

    fun syncContacts(phoneNumbers: List<String>) {
        viewModelScope.launch {
            val hasCache = _state.value.contacts.isNotEmpty()

            if (hasCache) {
                _state.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _state.update { it.copy(isLoading = true, error = null) }
            }

            repository.syncContacts(phoneNumbers)
                .onSuccess { syncedData ->
                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isOfflineMode = false,
                            contacts = syncedData,
                            filteredContacts = filterList(syncedData, currentState.searchQuery),
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to sync contacts with server"

                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isOfflineMode = hasCache,
                            error = if (hasCache) null else errorMsg
                        )
                    }

                    if (hasCache) {
                        _sideEffect.send(ContactsSideEffect.ShowToast("Showing saved contacts (Offline)"))
                    } else {
                        _sideEffect.send(ContactsSideEffect.ShowToast(errorMsg))
                    }
                }
        }
    }

    private fun updateSearch(query: String) {
        _state.update { currentState ->
            currentState.copy(
                searchQuery = query,
                filteredContacts = filterList(currentState.contacts, query)
            )
        }
    }

    private fun filterList(contacts: List<AuthResponse>, query: String): List<AuthResponse> {
        if (query.isBlank()) return contacts
        val cleanQuery = query.trim().removePrefix("@")
        return contacts.filter { contact ->
            contact.userName?.contains(cleanQuery, ignoreCase = true) == true ||
                    contact.phoneNumber?.contains(cleanQuery) == true
        }
    }
}