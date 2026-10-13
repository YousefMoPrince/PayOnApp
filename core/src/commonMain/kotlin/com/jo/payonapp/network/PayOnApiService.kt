package com.jo.payonapp.network

import com.jo.payonapp.dto.*
interface PayOnApiService {
    suspend fun register(request: RegisterRequest): GeneralApiResponse<AuthResponse>
    suspend fun registerUserDevice(request: RegisterUserDeviceRequest): GeneralApiResponse<String>
    suspend fun unRegisterUserDevice(deviceToken : String): GeneralApiResponse<String>
    suspend fun login(request: LoginRequest): GeneralApiResponse<AuthResponse>
    suspend fun refreshToken(map: Map<String, String>): Map<String, String>
    suspend fun getUserProfile(): GeneralApiResponse<AuthResponse>
    suspend fun updateProfile(request: UpdateRequest): GeneralApiResponse<AuthResponse>
    suspend fun updateUserStatus(status: UserStatus): String
    suspend fun uploadThumbnail(fileBytes: ByteArray, fileName: String): GeneralApiResponse<ThumbnailResponse>
    suspend fun downloadProfileImage(url: String): ByteArray
    suspend fun logout(request: LogoutRequest)
    suspend fun createWallet() : GeneralApiResponse<WalletResponse>
    suspend fun syncContacts(contacts : List<String>) : GeneralApiResponse<List<AuthResponse>>
    suspend fun getWalletInfo() : GeneralApiResponse<WalletResponse>
    suspend fun deposit(idempotencyKey: String, request: TransactionRequest): GeneralApiResponse<TransactionResponse>
    suspend fun withdraw(idempotencyKey: String, request: TransactionRequest): GeneralApiResponse<TransactionResponse>
    suspend fun transfer(idempotencyKey: String, request: TransactionRequest): GeneralApiResponse<TransactionResponse>
    suspend fun transferAck(idempotencyKey: String, state: Status): GeneralApiResponse<TransactionResponse>
    suspend fun getTransaction(id: String): TransactionResponse
    suspend fun getTransactions(): TransactionListResponse
    suspend fun becomeMerchant(request: BecomeMerchantRequest): GeneralApiResponse<MerchantResponse>
    suspend fun getMerchantInfo(): GeneralApiResponse<MerchantResponse>
    suspend fun createReceipt(request: CreateReceiptRequest): GeneralApiResponse<CreatedReceiptResponse>
    suspend fun getCreatedReceipt(transactionId: String): CreatedReceiptResponse
    suspend fun getReceipts(): List<ReceiptResponse>
    suspend fun getMerchantReceipts(): GeneralApiResponse<List<CreatedReceiptResponse>>
    suspend fun getEnterpriseReceipts(): List<CreatedReceiptResponse>
    suspend fun getReceipt(transactionCode: Long): GeneralApiResponse<ReceiptResponse>
    suspend fun payReceipt(transactionCode: Long): GeneralApiResponse<String>
    suspend fun getNotification(id: String): NotificationResponse
    suspend fun getNotifications(page: Int, size: Int): NotificationPageResponse
    suspend fun getUnreadNotifications(): Long
    suspend fun readNotification(notificationId: String)
    suspend fun readAllNotification()
}