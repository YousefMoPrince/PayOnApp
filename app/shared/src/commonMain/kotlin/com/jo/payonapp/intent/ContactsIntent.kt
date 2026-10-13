package com.jo.payonapp.intent

sealed class ContactsIntent {
    object LoadCachedContacts : ContactsIntent()
    data class SyncContacts(val phoneNumbers: List<String>) : ContactsIntent()
    data class UpdateSearchQuery(val query: String) : ContactsIntent()
    object RefreshContacts : ContactsIntent()
}