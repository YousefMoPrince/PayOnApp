package com.jo.payonapp.reposervice

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.jo.payonapp.api.TokenManager
import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.local.PayOnDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqliteTokenManager(
    database: PayOnDatabase
) : TokenManager {

    private val queries = database.userSessionQueries

    override fun saveSession(authResponse: AuthResponse) {
        queries.insertOrUpdateSession(
            userId = authResponse.userId,
            userName = authResponse.userName?:"",
            email = authResponse.email,
            phoneNumber = authResponse.phoneNumber,
            fullName = authResponse.fullName,
            thumbnailPath = authResponse.thumbnailPath,
            createdAt = authResponse.createdAt ?: "",
            updatedAt = authResponse.updatedAt ?: "",
            token = authResponse.token ?: "",
            refreshToken = authResponse.refreshToken ?: ""
        )
    }

    override fun getSession(): AuthResponse? {
        val entity = queries.getUserSession().executeAsOneOrNull() ?: return null
        return AuthResponse(
            userId = entity.userId ?: "",
            userName = entity.userName,
            email = entity.email ?: "",
            phoneNumber = entity.phoneNumber,
            fullName = entity.fullName,
            thumbnailPath = entity.thumbnailPath,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            token = entity.token,
            refreshToken = entity.refreshToken
        )
    }

    override fun updateProfile(authResponse: AuthResponse) {
        queries.updateProfileInfo(
            userName = authResponse.userName?:"",
            email = authResponse.email,
            phoneNumber = authResponse.phoneNumber,
            fullName = authResponse.fullName,
            thumbnailPath = authResponse.thumbnailPath,
            updatedAt = authResponse.updatedAt ?: ""
        )
    }

    override fun updateTokens(accessToken: String, refreshToken: String) {
        queries.updateTokens(
            token = accessToken,
            refreshToken = refreshToken,
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    override fun getAccessToken(): String? {
        return queries.getAccessToken().executeAsOneOrNull()
    }

    override fun getRefreshToken(): String? {
        return queries.getRefreshToken().executeAsOneOrNull()
    }

    override fun clearSession() {
        queries.deleteSession()
    }

    override fun getProfileFlow(): Flow<AuthResponse?> {
        return queries.getUserSession().asFlow().mapToOneOrNull(Dispatchers.IO).map { entity ->
            entity?.run {
                AuthResponse(
                    userId = userId ?: "",
                    userName = userName,
                    email = email ?: "",
                    phoneNumber = phoneNumber,
                    fullName = fullName,
                    thumbnailPath = thumbnailPath,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    token = token,
                    refreshToken = refreshToken
                )
            }
        }
    }

    override fun saveDeviceToken(token: String, deviceType: String) {
        queries.insertOrUpdateDeviceToken(token = token, deviceType = deviceType)
    }

    override fun getDeviceToken(): Pair<String, String>? {
        val entity = queries.getDeviceToken().executeAsOneOrNull() ?: return null
        return Pair(entity.token, entity.deviceType)
    }

    override fun clearDeviceToken() {
        queries.deleteDeviceToken()
    }
}