package com.jo.payonapp.api

import com.jo.payonapp.dto.AuthResponse
import kotlinx.coroutines.flow.Flow

interface TokenManager {
    fun saveSession(authResponse: AuthResponse)
    fun getSession(): AuthResponse?
    fun updateProfile(authResponse: AuthResponse)
    fun updateTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearSession()
    fun getProfileFlow(): Flow<AuthResponse?>

    fun saveDeviceToken(token: String, deviceType: String = "ANDROID")
    fun getDeviceToken(): Pair<String, String>?
    fun clearDeviceToken()
}