package com.jo.payonapp.repository

import com.jo.payonapp.dto.AuthResponse

interface ContactsRepository {
    suspend fun getContacts(): Result<List<AuthResponse>>
    suspend fun syncContacts(phoneNumbers: List<String>): Result<List<AuthResponse>>
}