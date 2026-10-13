package com.jo.payonapp.reposervice

import com.jo.payonapp.api.ContactManager
import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.local.PayOnDatabase

class SqliteContactManager(
    database: PayOnDatabase
) : ContactManager {
    private val queries = database.contactEntityQueries

    override fun saveContacts(contacts: List<AuthResponse>) {
        queries.transaction {
            queries.deleteAllContacts()
            contacts.forEach { user ->
                queries.insertOrUpdateContact(
                    userId = user.userId?:"",
                    userName = user.userName?:"",
                    email = user.email,
                    phoneNumber = user.phoneNumber,
                    fullName = user.fullName,
                    thumbnailPath = user.thumbnailPath
                )
            }
        }
    }

    override fun getContacts(): List<AuthResponse> {
        return queries.getAllContacts().executeAsList().map { entity ->
            AuthResponse(
                userId = entity.userId,
                userName = entity.userName,
                email = entity.email ?: "",
                phoneNumber = entity.phoneNumber,
                fullName = entity.fullName,
                thumbnailPath = entity.thumbnailPath,
                createdAt = null,
                updatedAt = null,
                token = null,
                refreshToken = null
            )
        }
    }

    override fun clearContacts() {
        queries.deleteAllContacts()
    }
}