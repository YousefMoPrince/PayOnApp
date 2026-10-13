package com.jo.payonapp.api

import com.jo.payonapp.dto.*
import com.jo.payonapp.network.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class PayOnServices(private val client: HttpClient) : PayOnApiService {

    override suspend fun register(request: RegisterRequest): GeneralApiResponse<AuthResponse> =
        client.post(NetworkConstants.Endpoints.REGISTER) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun registerUserDevice(request: RegisterUserDeviceRequest): GeneralApiResponse<String> =
        client.post(NetworkConstants.Endpoints.REGISTER_USER_DEVICE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun unRegisterUserDevice(deviceToken: String): GeneralApiResponse<String> {
        return client.delete("/api/v2/notifications/device/$deviceToken/delete").body()
    }

    override suspend fun login(request: LoginRequest): GeneralApiResponse<AuthResponse> =
        client.post(NetworkConstants.Endpoints.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun refreshToken(map: Map<String, String>): Map<String, String> =
        client.post(NetworkConstants.Endpoints.Refresh_Token) {
            contentType(ContentType.Application.Json)
            setBody(map)
        }.body()

    override suspend fun getUserProfile(): GeneralApiResponse<AuthResponse> =
        client.get(NetworkConstants.Endpoints.USER_PROFILE).body()

    override suspend fun updateProfile(request: UpdateRequest): GeneralApiResponse<AuthResponse> =
        client.put(NetworkConstants.Endpoints.UPDATE_PROFILE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun updateUserStatus(status: UserStatus): String =
        client.patch(NetworkConstants.Endpoints.UPDATE_USER_STATUS + status.name).body()


    override suspend fun uploadThumbnail(fileBytes: ByteArray, fileName: String): GeneralApiResponse<ThumbnailResponse> =
        client.submitFormWithBinaryData(
            url = NetworkConstants.Endpoints.UPLOAD_THUMBNAIL,
            formData = formData {
                append("file", fileBytes, Headers.Companion.build {
                    append(HttpHeaders.ContentDisposition, "filename=$fileName")
                })
            }
        ).body()

    override suspend fun downloadProfileImage(url: String): ByteArray =
        client.get(url).body()

    override suspend fun logout(request: LogoutRequest) {
        client.post(NetworkConstants.Endpoints.LOGOUT) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun createWallet(): GeneralApiResponse<WalletResponse> =
        client.post(NetworkConstants.Endpoints.CREATE_WALLET).body()

    override suspend fun syncContacts(contacts: List<String>): GeneralApiResponse<List<AuthResponse>> =
        client.post(NetworkConstants.Endpoints.SYNC_CONTACTS) {
            contentType(ContentType.Application.Json)
            setBody(contacts)
        }.body()

    override suspend fun getWalletInfo(): GeneralApiResponse<WalletResponse> =
        client.get(NetworkConstants.Endpoints.WALLET_INFO).body()

    override suspend fun deposit(idempotencyKey: String, request: TransactionRequest): GeneralApiResponse<TransactionResponse> =
        client.post(NetworkConstants.Endpoints.DEPOSIT) {
            contentType(ContentType.Application.Json)
            header("Idempotency-Key", idempotencyKey)
            setBody(request)
        }.body()

    override suspend fun withdraw(idempotencyKey: String, request: TransactionRequest): GeneralApiResponse<TransactionResponse> =
        client.post(NetworkConstants.Endpoints.WITHDRAW) {
            contentType(ContentType.Application.Json)
            header("Idempotency-Key", idempotencyKey)
            setBody(request)
        }.body()

    override suspend fun transfer(idempotencyKey: String, request: TransactionRequest): GeneralApiResponse<TransactionResponse> =
        client.post(NetworkConstants.Endpoints.TRANSFER) {
            contentType(ContentType.Application.Json)
            header("Idempotency-Key", idempotencyKey)
            setBody(request)
        }.body()

    override suspend fun transferAck(idempotencyKey: String, state: Status): GeneralApiResponse<TransactionResponse> =
        client.put(NetworkConstants.Endpoints.TRANSFER_ACK) {
            parameter("idempotencyKey", idempotencyKey)
            parameter("state", state.name)
        }.body()

    override suspend fun getTransaction(id: String): TransactionResponse =
        client.get(NetworkConstants.Endpoints.GET_TRANSACTION){
            parameter("transactionId",id)
        }.body()

    override suspend fun getTransactions(): TransactionListResponse =
        client.get(NetworkConstants.Endpoints.GET_TRANSACTIONS).body()

    override suspend fun becomeMerchant(request: BecomeMerchantRequest): GeneralApiResponse<MerchantResponse> =
        client.post(NetworkConstants.Endpoints.BECOME_MERCHANT) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun getMerchantInfo(): GeneralApiResponse<MerchantResponse> =
        client.get(NetworkConstants.Endpoints.GET_MERCHANT_INFO).body()

    override suspend fun getReceipts(): List<ReceiptResponse> =
        client.get(NetworkConstants.Endpoints.GET_RECEIPTS).body()

    override suspend fun getMerchantReceipts(): GeneralApiResponse<List<CreatedReceiptResponse>> {
        return client.get(NetworkConstants.Endpoints.GET_MERCHANT_RECEIPTS).body()
    }

    override suspend fun getEnterpriseReceipts(): List<CreatedReceiptResponse> {
        return client.get(NetworkConstants.Endpoints.GET_ENTERPRISE_RECEIPTS).body()
    }

    override suspend fun createReceipt(request: CreateReceiptRequest): GeneralApiResponse<CreatedReceiptResponse> =
        client.post(NetworkConstants.Endpoints.CREATE_RECEIPT) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun getCreatedReceipt(transactionId: String): CreatedReceiptResponse =
        client.get(NetworkConstants.Endpoints.GET_CREATED_RECEIPT) {
            parameter("transactionId", transactionId)
        }.body()

    override suspend fun getReceipt(transactionCode: Long): GeneralApiResponse<ReceiptResponse> =
        client.get("api/v2/onPay/receipts/$transactionCode").body()

    override suspend fun payReceipt(transactionCode: Long): GeneralApiResponse<String> =
        client.post("api/v2/onPay/receipts/$transactionCode/pay").body()

    override suspend fun getNotification(id: String): NotificationResponse =
        client.get("/api/v2/notifications/show/$id").body()

    override suspend fun getNotifications(page: Int, size: Int): NotificationPageResponse =
        client.get(NetworkConstants.Endpoints.NOTIFICATIONS) {
            parameter("page", page)
            parameter("size", size)
        }.body()

    override suspend fun getUnreadNotifications(): Long =
        client.get(NetworkConstants.Endpoints.UN_READ_NOTIFICATIONS).body()

    override suspend fun readNotification(notificationId: String) {
        client.patch(NetworkConstants.Endpoints.READ_NOTIFICATION + notificationId)
    }
    override suspend fun readAllNotification() {
        client.patch(NetworkConstants.Endpoints.READ_ALL_NOTIFICATION)
    }
}