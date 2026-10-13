package com.jo.payonapp.repository

import com.jo.payonapp.dto.AuthResponse
import com.jo.payonapp.dto.GeneralApiResponse
import com.jo.payonapp.dto.LoginRequest
import com.jo.payonapp.dto.RegisterRequest
import com.jo.payonapp.dto.RegisterUserDeviceRequest
import com.jo.payonapp.dto.UpdateRequest
import com.jo.payonapp.dto.UserStatus

interface AuthRepository {
    suspend fun login(request: LoginRequest): Result<AuthResponse>
    suspend fun register(request: RegisterRequest): Result<AuthResponse>
    suspend fun getUserProfile(): Result<AuthResponse>
    suspend fun refreshUserProfile(): Result<AuthResponse>
    suspend fun registerUserDevice(request: RegisterUserDeviceRequest): Result<GeneralApiResponse<String>>
    suspend fun unRegisterUserDevice(deviceToken: String): Result<GeneralApiResponse<String>>
    suspend fun updateProfile(request: UpdateRequest): Result<AuthResponse>
    suspend fun updateUserStatus(status: UserStatus): Result<String>
    suspend fun uploadThumbnail(fileBytes: ByteArray, fileName: String): Result<String>
    suspend fun fetchProfileImageFromServer(imageUrl: String, saveFileName: String): Result<String>
    suspend fun getProfileImageFromLocal(filePath: String): Result<ByteArray>
    suspend fun logout(): Result<Unit>
}