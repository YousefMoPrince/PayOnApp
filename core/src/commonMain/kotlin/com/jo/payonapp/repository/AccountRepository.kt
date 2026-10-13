package com.jo.payonapp.repository

import com.jo.payonapp.dto.AuthResponse

interface AccountRepository {
    suspend fun getUserProfile(): Result<AuthResponse>
    suspend fun refreshUserProfile(): Result<AuthResponse>
    suspend fun getDecryptedThumbnail(fileName: String, imageUrl: String?): Result<ByteArray>
    suspend fun logout(): Result<Unit>
}