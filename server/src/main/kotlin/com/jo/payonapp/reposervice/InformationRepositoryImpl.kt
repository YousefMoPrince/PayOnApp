package com.jo.payonapp.reposervice

import com.jo.payonapp.api.TokenManager
import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.dto.LogoutRequest
import com.jo.payonapp.dto.UpdateRequest
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.AuthRepository
import com.jo.payonapp.repository.InformationRepository
import com.jo.payonapp.storage.LocalImageStorage

class InformationRepositoryImpl(
    private val apiService: PayOnApiService,
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val localImageStorage: LocalImageStorage
) : InformationRepository {

    override suspend fun getUserProfile(): Result<AuthResponse> {
        val cached = tokenManager.getSession()
        if (cached != null) {
            return Result.success(cached)
        }
        return refreshUserProfile()
    }

    override suspend fun refreshUserProfile(): Result<AuthResponse> {
        return runCatching {
            val response = apiService.getUserProfile()
            val userData = response.data ?: throw Exception(response.message)
            tokenManager.updateProfile(userData)
            userData
        }.recoverCatching { networkException ->
            tokenManager.getSession() ?: throw networkException
        }
    }

    override suspend fun updateProfile(request: UpdateRequest): Result<AuthResponse> {
        return runCatching {
            val response = apiService.updateProfile(request)
            val updatedUser = response.data ?: throw Exception(response.message)
            tokenManager.updateProfile(updatedUser)
            updatedUser
        }
    }

    override suspend fun uploadThumbnail(fileBytes: ByteArray, fileName: String): Result<String> {
        return authRepository.uploadThumbnail(fileBytes, fileName)
    }

    override suspend fun getDecryptedThumbnail(fileName: String, imageUrl: String?): Result<ByteArray> {
        return runCatching {
            try {
                localImageStorage.getDecryptedImage(fileName)
            } catch (e: Exception) {
                if (!imageUrl.isNullOrBlank()) {
                    val downloadedBytes = apiService.downloadProfileImage(imageUrl)
                    localImageStorage.saveEncryptedImage(fileName, downloadedBytes)
                    localImageStorage.getDecryptedImage(fileName)
                } else {
                    throw e
                }
            }
        }
    }

    override suspend fun logout(): Result<Unit> {
        return runCatching {
            val refreshToken = tokenManager.getRefreshToken()
            if (!refreshToken.isNullOrEmpty()) {
                apiService.logout(LogoutRequest(refreshToken))
            }
        }.also {
            tokenManager.clearSession()
        }
    }
}