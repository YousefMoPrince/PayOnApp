package com.jo.payonapp.reposervice

import com.jo.payonapp.api.TokenManager
import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.dto.GeneralApiResponse
import com.jo.payonapp.dto.LoginRequest
import com.jo.payonapp.dto.LogoutRequest
import com.jo.payonapp.dto.RegisterRequest
import com.jo.payonapp.dto.RegisterUserDeviceRequest
import com.jo.payonapp.dto.UpdateRequest
import com.jo.payonapp.dto.UserStatus
import com.jo.payonapp.network.PayOnApiService
import com.jo.payonapp.repository.AuthRepository
import com.jo.payonapp.repository.NotificationRepository
import com.jo.payonapp.storage.LocalImageStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens

class AuthRepositoryImpl(
    private val apiService: PayOnApiService,
    private val tokenManager: TokenManager,
    private val notificationRepository: NotificationRepository,
    private val localImageStorage: LocalImageStorage,
    private val client: HttpClient
) : AuthRepository {

    override suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return runCatching {
            val response = apiService.login(request)
            val authData = response.data ?: throw Exception(response.message)
            tokenManager.saveSession(authData)
            client.clearAuthTokens()
            tokenManager.getDeviceToken()?.let { (deviceToken, deviceType) ->
                apiService.registerUserDevice(RegisterUserDeviceRequest(deviceToken, deviceType))
            }
            notificationRepository.startNotificationListener()
            authData
        }
    }

    override suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return runCatching {
            val response = apiService.register(request)
            val authData = response.data ?: throw Exception(response.message)

            tokenManager.saveSession(authData)
            client.clearAuthTokens()
            tokenManager.getDeviceToken()?.let { (deviceToken, deviceType) ->
                apiService.registerUserDevice(RegisterUserDeviceRequest(deviceToken, deviceType))
            }
            notificationRepository.startNotificationListener()
            authData
        }
    }

    override suspend fun getUserProfile(): Result<AuthResponse> {
        return runCatching {
            tokenManager.getSession() ?: throw Exception("No User Info Found")
        }.recoverCatching {
            val response = apiService.getUserProfile()
            val userData = response.data ?: throw Exception(response.message)
            tokenManager.updateProfile(userData)
            userData
        }
    }

    override suspend fun refreshUserProfile(): Result<AuthResponse> {
        return runCatching {
            val response = apiService.getUserProfile()
            val userData = response.data ?: throw Exception(response.message)
            tokenManager.updateProfile(userData)
            userData
        }
    }

    override suspend fun registerUserDevice(request: RegisterUserDeviceRequest): Result<GeneralApiResponse<String>> {
        return runCatching {
            if (tokenManager.getDeviceToken() == null) {
                tokenManager.saveDeviceToken(request.deviceToken, request.deviceType)
            }

            if (!tokenManager.getAccessToken().isNullOrEmpty()) {
                apiService.registerUserDevice(request)
            } else {
                GeneralApiResponse(message = "Saved locally until user authenticates", data = null)
            }
        }
    }

    override suspend fun unRegisterUserDevice(deviceToken: String): Result<GeneralApiResponse<String>> {
        return runCatching {
            val response = apiService.unRegisterUserDevice(deviceToken)
            tokenManager.clearDeviceToken()
            response
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

    override suspend fun updateUserStatus(status: UserStatus): Result<String> {
        return runCatching {
            apiService.updateUserStatus(status)
        }
    }

    override suspend fun uploadThumbnail(fileBytes: ByteArray, fileName: String): Result<String> {
        return runCatching {
            val response = apiService.uploadThumbnail(fileBytes, fileName)
            fetchProfileImageFromServer(response.data!!.thumbnailPath, response.data!!.thumbnailName)
            getUserProfile()
            response.data?.thumbnailPath ?: throw Exception(response.message)
        }
    }

    override suspend fun fetchProfileImageFromServer(imageUrl: String, saveFileName: String): Result<String> {
        return runCatching {
            val imageBytes = apiService.downloadProfileImage(imageUrl)
            localImageStorage.saveEncryptedImage(saveFileName, imageBytes)
        }
    }

    override suspend fun getProfileImageFromLocal(filePath: String): Result<ByteArray> {
        return runCatching {
            localImageStorage.getDecryptedImage(filePath)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return runCatching {
            tokenManager.getDeviceToken()?.let { (deviceToken, _) ->
                unRegisterUserDevice(deviceToken)
            }

            val refreshToken = tokenManager.getRefreshToken()
            if (!refreshToken.isNullOrEmpty()) {
                updateUserStatus(UserStatus.OFFLINE)
                apiService.logout(LogoutRequest(refreshToken))
            }
        }.also {
            notificationRepository.stopNotificationListener()
            tokenManager.clearSession()
        }
    }
}