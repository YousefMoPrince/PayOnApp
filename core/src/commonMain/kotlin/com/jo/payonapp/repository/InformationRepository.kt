package com.jo.payonapp.repository

import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.dto.UpdateRequest

interface InformationRepository {
    suspend fun getUserProfile(): Result<AuthResponse>
    suspend fun refreshUserProfile(): Result<AuthResponse>
    suspend fun updateProfile(request: UpdateRequest): Result<AuthResponse>
    suspend fun uploadThumbnail(fileBytes: ByteArray, fileName: String): Result<String>
    suspend fun getDecryptedThumbnail(fileName: String, imageUrl: String?): Result<ByteArray>
    suspend fun logout(): Result<Unit>
}