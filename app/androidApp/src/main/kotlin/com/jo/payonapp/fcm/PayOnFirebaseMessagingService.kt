package com.jo.payonapp.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jo.payonapp.dto.RegisterUserDeviceRequest
import com.jo.payonapp.notification.PayOnNotificationHelper
import com.jo.payonapp.repository.AuthRepository
import com.jo.payonapp.repository.NotificationRepository
import com.jo.payonapp.repository.OnPayRepository
import com.jo.payonapp.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext

class PayOnFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val notificationHelper by lazy { PayOnNotificationHelper(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Deprecated("Deprecated in Java")
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (GlobalContext.getOrNull() != null) {
            serviceScope.launch {
                try {
                    val authRepository: AuthRepository = get()
                    authRepository.registerUserDevice(
                        RegisterUserDeviceRequest(
                            deviceToken = token,
                            deviceType = "ANDROID"
                        )
                    )
                } catch (e: Exception) {
                    Log.e("FCM_SERVICE", "Failed to register FCM token", e)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        if (data.isEmpty() || GlobalContext.getOrNull() == null) return

        val notificationId = data["notification_id"] ?: data["notificationId"]
        val transactionId = data["transaction_id"] ?: data["transactionId"]
        val transactionType = data["transaction_type"] ?: data["transactionType"]

        if (!notificationId.isNullOrEmpty()) {
            runBlocking {
                try {
                    val notificationRepository: NotificationRepository = get()
                    val transactionRepository: TransactionRepository = get()
                    val onPayRepository: OnPayRepository = get()

                    val result = notificationRepository.getNotification(notificationId)

                    result.onSuccess { notification ->
                        if (!transactionId.isNullOrEmpty()) {
                            runCatching {
                                if (transactionType == "TRANSACTION") {
                                    transactionRepository.getTransaction(transactionId)
                                } else {
                                    onPayRepository.getCreatedReceipt(transactionId)
                                }
                            }
                        }
                        notificationHelper.makeFCMNotification(
                            notificationId = notificationId,
                            transactionId = transactionId,
                            title = notification.title,
                            eventType = notification.eventType,
                            message = notification.message
                        )
                    }.onFailure { throwable ->
                        Log.e("FCM_SERVICE", "API Call failed for notificationId: $notificationId", throwable)
                    }

                } catch (e: Exception) {
                    Log.e("FCM_SERVICE", "Error executing task in FCM", e)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}