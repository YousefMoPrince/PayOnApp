package com.jo.payonapp.reposervice

import com.jo.payonapp.api.ContactManager
import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.ContactsRepository

class ContactsRepositoryImpl(
    private val apiService: PayOnApiService,
    private val contactManager: ContactManager
) : ContactsRepository {

    override suspend fun getContacts(): Result<List<AuthResponse>> {
        return runCatching {
            contactManager.getContacts()
        }
    }

    override suspend fun syncContacts(phoneNumbers: List<String>): Result<List<AuthResponse>> {
        return runCatching {
            if (phoneNumbers.isEmpty()) {
                return@runCatching emptyList()
            }
            val response = apiService.syncContacts(phoneNumbers)
            val syncedContacts = response.data ?: emptyList()

            if (syncedContacts.isNotEmpty()) {
                contactManager.saveContacts(syncedContacts)
            }
            syncedContacts
        }
    }
}