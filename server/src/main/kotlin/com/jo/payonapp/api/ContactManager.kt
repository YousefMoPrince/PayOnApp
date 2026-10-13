package com.jo.payonapp.api

import com.jo.payonapp.dto.AuthResponse

interface ContactManager {
    fun saveContacts(contacts: List<AuthResponse>)
    fun getContacts(): List<AuthResponse>
    fun clearContacts()
}